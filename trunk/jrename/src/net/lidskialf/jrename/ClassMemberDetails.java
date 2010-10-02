package net.lidskialf.jrename;

public class ClassMemberDetails {
	public String name;
	public String returnDesc;
	public String argsDesc;

	public ClassMemberDetails(String oldName, String typeDesc) { /* for a field */
		this(oldName, typeDesc, null);
	}

	public ClassMemberDetails(String oldName, String returnDesc, String argsDesc) { /* for a method */
		this.name = oldName;
		this.returnDesc = returnDesc;
		this.argsDesc = argsDesc;
	}
}
