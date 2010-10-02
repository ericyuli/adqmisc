package net.lidskialf.jrename;

import org.objectweb.asm.signature.SignatureVisitor;

public class Phase3DeObfuscatorSignatureVisitor implements SignatureVisitor {

	private ClassProcessor cp;
	private SignatureVisitor sv;
	
	public Phase3DeObfuscatorSignatureVisitor(ClassProcessor cp, SignatureVisitor sv) {
		this.cp = cp;
		this.sv = sv;
	}
	
	@Override
	public SignatureVisitor visitArrayType() {
		sv.visitArrayType();
		return this;
	}

	@Override
	public void visitBaseType(char descriptor) {
		sv.visitBaseType(descriptor);	
	}

	@Override
	public SignatureVisitor visitClassBound() {
		sv.visitClassBound();
		return this;
	}

	@Override
	public void visitClassType(String name) {
		name = cp.FixClassName(name);
		sv.visitClassType(name);
	}

	@Override
	public void visitEnd() {
		sv.visitEnd();
	}

	@Override
	public SignatureVisitor visitExceptionType() {
		sv.visitExceptionType();
		return this;
	}

	@Override
	public void visitFormalTypeParameter(String name) {
		sv.visitFormalTypeParameter(name);
	}

	@Override
	public void visitInnerClassType(String name) {
		name = cp.FixClassName(name);
		sv.visitInnerClassType(name);
	}

	@Override
	public SignatureVisitor visitInterface() {
		sv.visitInterface();
		return this;
	}

	@Override
	public SignatureVisitor visitInterfaceBound() {
		sv.visitInterfaceBound();
		return this;
	}

	@Override
	public SignatureVisitor visitParameterType() {
		sv.visitParameterType();
		return this;
	}

	@Override
	public SignatureVisitor visitReturnType() {
		sv.visitReturnType();
		return this;
	}

	@Override
	public SignatureVisitor visitSuperclass() {
		sv.visitSuperclass();
		return this;
	}

	@Override
	public void visitTypeArgument() {
		sv.visitTypeArgument();
	}

	@Override
	public SignatureVisitor visitTypeArgument(char wildcard) {
		sv.visitTypeArgument(wildcard);
		return this;
	}

	@Override
	public void visitTypeVariable(String name) {
		sv.visitTypeVariable(name);
	}
}
