package com.github.gitantoinee.deobfuscator.radon.visitors.flow

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

public class RadonGotoReplacerMethodVisitor(
    /**
     * The possible predicate fields
     */
    private val possiblePredicateFields: List<String> = emptyList(),
    inner: MethodVisitor? = null,
) : MethodVisitor(Opcodes.ASM9, inner) {
    /**
     * The index of variable used as operand for IFEQ opcodes
     */
    private var predicateVar: Int? = null

    /**
     * The predicate field
     */
    private var predicateField: String? = null

    /**
     * The predicate variable is loaded
     */
    private var predicateLoaded: Boolean = false

    /**
     * The current label
     */
    private var currentLabel: Label? = null

    override fun visitLabel(label: Label) {
        currentLabel = label
        super.visitLabel(label)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        predicateLoaded = Opcodes.ILOAD == opcode && predicateField != null && predicateVar == `var`

        if (Opcodes.ISTORE == opcode && predicateVar == null && predicateField != null && currentLabel == null) {
            predicateVar = `var`
        } else if(!predicateLoaded) {
            super.visitVarInsn(opcode, `var`)
        }
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        if (!predicateLoaded) {
            super.visitJumpInsn(opcode, label)
            return
        }

        when (opcode) {
            Opcodes.IFEQ -> super.visitJumpInsn(Opcodes.GOTO, label)
            Opcodes.IFNE -> Unit // Just continue
            else -> {
                println("Unknown opcode $opcode to $label")
                super.visitVarInsn(Opcodes.ILOAD, predicateVar!!)
                super.visitJumpInsn(opcode, label)
            }
        }
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        if (Opcodes.GETSTATIC == opcode
            && (possiblePredicateFields.isEmpty() || name in possiblePredicateFields)
            && descriptor == "Z"
            && predicateVar == null && predicateField == null && currentLabel == null
        ) {
            predicateField = name
        } else {
            super.visitFieldInsn(opcode, owner, name, descriptor)
        }
    }
}
