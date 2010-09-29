package net.lidskialf.jrename;

import java.util.HashMap;

import org.objectweb.asm.*;

public class DeObfuscatorMethodVisitor implements MethodVisitor {
	
	public static boolean StripLineNumbers = true;
	
	private ClassProcessor cp;
	private int methodAccess;
	private MethodVisitor mv;	
	private HashMap<String, Integer> localVariableNames = new HashMap<String, Integer>();
	private HashMap<Integer, String> localVariableSlots = new HashMap<Integer, String>();
	
	public DeObfuscatorMethodVisitor(ClassProcessor cp, int methodAccess, MethodVisitor mv) {
		this.cp = cp;
		this.methodAccess = methodAccess;
		this.mv = mv;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return mv.visitAnnotation(desc, visible);
	}

	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		return mv.visitAnnotationDefault();
	}

	@Override
	public void visitAttribute(Attribute attr) {
		mv.visitAttribute(attr);
	}

	@Override
	public void visitCode() {
		mv.visitCode();
	}

	@Override
	public void visitEnd() {
		mv.visitEnd();
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		String ownerOldName = owner;
		owner = cp.FixClassName(ownerOldName);
		name = cp.FixFieldName(ownerOldName, name, desc);
		desc = cp.FixDescriptor(desc);
		mv.visitFieldInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		mv.visitFrame(type, nLocal, local, nStack, stack);
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		mv.visitIincInsn(var, increment);
	}

	@Override
	public void visitInsn(int opcode) {
		mv.visitInsn(opcode);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		mv.visitIntInsn(opcode, operand);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		mv.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitLabel(Label label) {
		mv.visitLabel(label);
	}

	@Override
	public void visitLdcInsn(Object cst) {
		mv.visitLdcInsn(cst);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		if (!StripLineNumbers)
			mv.visitLineNumber(line, start);
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature,
			Label start, Label end, int index) {
		
		if (!localVariableSlots.containsKey(index)) {
			if ((index == 0) && ((methodAccess & Opcodes.ACC_STATIC) == 0))
				name = "this";
	
			String localNewName = name;
			if (cp.NeedsRenamed(name)) {
				localNewName = "local_" + name;
				
				String tmpLocalName;
				for(int idx = 0; ; idx++) {
					tmpLocalName = localNewName;
					if (idx > 0)
						tmpLocalName += idx;
	
					if (localVariableNames.containsKey(tmpLocalName))
						continue;
					break;
				}
				localNewName = tmpLocalName;
			}
			localVariableNames.put(localNewName, index);
			localVariableSlots.put(index, localNewName);
		}
		
		name = localVariableSlots.get(index);
		desc = cp.FixDescriptor(desc);
		
		if (signature != null)
			throw new RuntimeException("Signature was not null");
		
		mv.visitLocalVariable(name, desc, signature, start, end, index);		
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		mv.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		mv.visitMaxs(maxStack, maxLocals);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		String ownerOldName = owner;
		owner = cp.FixClassName(ownerOldName);
		name = cp.FixMethodName(ownerOldName, name, desc);
		desc = cp.FixMethodDescriptor(desc);
		mv.visitMethodInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		desc = cp.FixDescriptor(desc);
		mv.visitMultiANewArrayInsn(desc, dims);
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
		// FIXME: should we fix the desc here?
		throw new RuntimeException("visitParameterAnnotation called");
//		return mv.visitParameterAnnotation(parameter, desc, visible);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
		mv.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		if (type != null)
			type = cp.FixType(type);
		mv.visitTryCatchBlock(start, end, handler, type);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (type != null)
			type = cp.FixType(type);
		mv.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		mv.visitVarInsn(opcode, var);
	}
}
