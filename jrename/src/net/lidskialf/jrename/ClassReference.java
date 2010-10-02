package net.lidskialf.jrename;

public class ClassReference {

	public String fromClassName;
	public String toClassName;
	public String innerClassName;
	public boolean isInterface;
	
	public ClassReference(String fromClassName, String toClassName, String innerClassName, boolean isInterface) {
		this.fromClassName = fromClassName;
		this.toClassName = toClassName;
		this.innerClassName = innerClassName;
		this.isInterface = isInterface;
	}
}
