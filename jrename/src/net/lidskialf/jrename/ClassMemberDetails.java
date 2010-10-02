package net.lidskialf.jrename;

public class ClassMemberDetails {
	public String name;
	public String desc;
	public Object value;

	public ClassMemberDetails(String oldName, String desc, Object value) { /* for a field */
		this(oldName, desc);
		this.value = value;
	}

	public ClassMemberDetails(String oldName, String desc) { /* for a method */
		this.name = oldName;
		this.desc = desc;
	}
}
