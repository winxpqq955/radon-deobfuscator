package com.github.gitantoinee.deobfuscator.radon.visitors.flow

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

public class RadonLightFlowMethodVisitor(inner: MethodVisitor? = null) : MethodVisitor(Opcodes.ASM9, inner)