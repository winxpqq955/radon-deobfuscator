package com.github.gitantoinee.deobfuscator.radon.visitors.flow

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

public class RadonBogusJumpInserterMethodVisitor(
    /**
     * The owner, the name and the descriptor of the predicate field
     */
    private val predicateField: Triple<String, String, String>,
    inner: MethodVisitor? = null,
) : MethodVisitor(Opcodes.ASM9, inner) {
    private enum class State {
        /**
         * The predicate local variable is not found
         */
        DISABLED,

        /**
         * Find elements used in IDLE and REMOVING states
         */
        LOADING,

        /**
         * Wait for a bogus jump
         */
        IDLE,

        /**
         * Removing a bogus jump
         */
        PATCHING,
    }

    private var state: State = State.LOADING

    private var predicateVariableIndex: Int? = null

    private var exitLabel: Label? = null

    override fun visitLabel(label: Label) {
        // Set the state to IDLE when the label changes because the loading phase is only at the beginning of the method
        // when there is no labels. And the instructions of a bogus jump are only in a label
        // If the predicate variable is not found, then the deobfuscator cannot find bogus jump
        state = if (predicateVariableIndex != null) State.IDLE else State.DISABLED

        // The exit label is the first label
        if (State.IDLE == state && exitLabel == null) {
            exitLabel = label
        } else {
            super.visitLabel(label)
        }
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        if (State.LOADING == state && Opcodes.ISTORE == opcode) {
            check(predicateVariableIndex == null) { "Predicate variable found twice" }
            predicateVariableIndex = `var`

            // super.visitVarInsn(opcode, `var`)
        } else if (State.IDLE == state && Opcodes.ILOAD == opcode && predicateVariableIndex == `var`) {
            state = State.PATCHING
        } else {
            super.visitVarInsn(opcode, `var`)
        }
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        if (State.LOADING == state && Opcodes.GETSTATIC == opcode
            && owner == predicateField.first && name == predicateField.second && descriptor == predicateField.third
        ) {
            // super.visitFieldInsn(opcode, owner, name, descriptor)
        } else {
            super.visitFieldInsn(opcode, owner, name, descriptor)
        }
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        if (State.PATCHING == state && Opcodes.IFNE == opcode) {
            check(exitLabel == label) { "Attempt to remove non-obfuscated jump instruction" }

            // The instructions are removed
            state = State.IDLE
        } else {
            super.visitJumpInsn(opcode, label)
        }
    }

    override fun visitLocalVariable(
        name: String?,
        descriptor: String?,
        signature: String?,
        start: Label?,
        end: Label?,
        index: Int,
    ) {
        if (predicateVariableIndex != index) {
            super.visitLocalVariable(name, descriptor, signature, start, end, index)
        }
    }
}
