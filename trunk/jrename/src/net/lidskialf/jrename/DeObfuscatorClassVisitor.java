package net.lidskialf.jrename;
import java.util.Vector;

import org.objectweb.asm.*;

public class DeObfuscatorClassVisitor implements ClassVisitor {

	private String classNewFullName;
	private String classOriginalFullName;
	
	private ClassVisitor cv;
	private ClassProcessor cp;
	
	public DeObfuscatorClassVisitor(ClassProcessor cp, ClassVisitor cv) {
		this.cp = cp;
		this.cv = cv;
	}
	
	public String getClassNewFullName() {
		return classNewFullName;
	}

	@Override
	public void visit(int version, 
					  int access, 
					  String name, 
					  String signature,
					  String superName, 
					  String[] interfaces) {
		
		classOriginalFullName = name;
		
		name = cp.FixClassName(name);
		classNewFullName = name;
		superName = cp.FixClassName(superName);
		
		Vector<String> newInterfaces = new Vector<String>();
		for(String intf: interfaces) {
			newInterfaces.add(cp.FixClassName(intf));
		}
		interfaces = newInterfaces.toArray(new String[newInterfaces.size()]);
				
		// FIXME
		if (signature != null)
			throw new RuntimeException("Signature was not null");
		// END FIXME		
		
		cv.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, 
								   String name, 
								   String desc,
								   String signature, 
								   Object value) {
		
		name = cp.FixFieldName(classOriginalFullName, name);
		desc = cp.FixDescriptor(desc);

		// FIXME
		if (signature != null)
			throw new RuntimeException("Signature was not null");
		// END FIXME
		
		return cv.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, 
									 String name, 
									 String desc,
									 String signature, 
									 String[] exceptions) {
		
		name = cp.FixMethodName(classOriginalFullName, name, desc);
		desc = cp.FixMethodDescriptor(desc);

		if (exceptions != null) {
			Vector<String> newExceptions = new Vector<String>();
			for(String x: exceptions)
				newExceptions.add(cp.FixClassName(x));
			exceptions = newExceptions.toArray(new String[newExceptions.size()]);
		}
		
		// FIXME
		if (signature != null)
			throw new RuntimeException("Signature was not null");
		// END FIXME
		
		return new DeObfuscatorMethodVisitor(cp, access, cv.visitMethod(access, name, desc, signature, exceptions));
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		name = cp.FixClassName(name);
		if (outerName != null)
			outerName = cp.FixClassName(outerName);
		if (innerName != null)
			innerName = cp.FixClassName(innerName);
		
		cv.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {

		// FIXME
		throw new RuntimeException("Outer class seen");
		// END FIXME
		
//		cv.visitOuterClass(owner,  name, desc);
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
