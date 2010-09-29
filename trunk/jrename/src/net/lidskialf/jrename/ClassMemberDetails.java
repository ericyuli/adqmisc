package net.lidskialf.jrename;

public class ClassMemberDetails {
	public String oldName;
	public String newName;
	public String returnDesc;

	public ClassMemberDetails(String oldName, String newName) { /* for a field */
		this(oldName, newName, null);
	}

	public ClassMemberDetails(String oldName, String newName, String returnDesc) { /* for a method */
		this.oldName = oldName;
		this.newName = newName;
		this.returnDesc = returnDesc;
	}
}
