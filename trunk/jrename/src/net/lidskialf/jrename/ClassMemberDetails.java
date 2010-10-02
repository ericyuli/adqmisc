package net.lidskialf.jrename;

public class ClassMemberDetails {
	public String name;
	public String returnDesc;
	public String argsDesc;
	public Object value;

	public ClassMemberDetails(String oldName, String typeDesc, Object value) { /* for a field */
		this(oldName, typeDesc, null);
		this.value = value;
	}

	public ClassMemberDetails(String oldName, String returnDesc, String argsDesc) { /* for a method */
		this.name = oldName;
		this.returnDesc = returnDesc;
		this.argsDesc = argsDesc;
	}
}
