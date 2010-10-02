package net.lidskialf.jrename;

import org.objectweb.asm.*;

public class Phase1DeObfuscatorClassVisitor implements ClassVisitor {

	private String className;
	
	private ClassVisitor cv;
	private ClassProcessor cp;
	
	public Phase1DeObfuscatorClassVisitor(ClassProcessor cp, ClassVisitor cv) {
		this.cp = cp;
		this.cv = cv;
	}

	@Override
	public void visit(int version, 
					  int access, 
					  String name, 
					  String signature,
					  String superName, 
					  String[] interfaces) {
		
		this.className = name;		
		cp.AddClass(name, superName, interfaces, (access & Opcodes.ACC_INTERFACE) == 0);
		
		cv.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, 
								   String name, 
								   String desc,
								   String signature, 
								   Object value) {
		
		cp.AddField(className, name, desc, value);
		
		return cv.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, 
									 String name, 
									 String desc,
									 String signature, 
									 String[] exceptions) {
		cp.AddMethod(className, name, desc);
		
		return cv.visitMethod(access, name, desc, signature, exceptions);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		
		cp.AddInnerClassReference(outerName, name, innerName, (access & Opcodes.ACC_INTERFACE) == 0);
		
		cv.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		cp.AddOuterClassReference(owner, name);
		
		cv.visitOuterClass(owner, name, desc);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return cv.visitAnnotation(desc, visible);
	}

	@Override
	public void visitAttribute(Attribute attr) {
		cv.visitAttribute(attr);		
	}

	@Override
	public void visitEnd() {
		cv.visitEnd();
	}

	@Override
	public void visitSource(String source, String debug) {
		cv.visitSource(source, debug);		
	}
}
