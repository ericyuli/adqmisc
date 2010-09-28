package net.lidskialf.jrename;

import java.util.HashMap;

public class ClassDetails {
	
	public String oldName;
	public String newName;
	
	public HashMap<String, ClassMemberDetails> oldFieldNames = new HashMap<String, ClassMemberDetails>();
	public HashMap<String, ClassMemberDetails> newFieldNames = new HashMap<String, ClassMemberDetails>();

	public HashMap<String, ClassMemberDetails> oldMethodNames = new HashMap<String, ClassMemberDetails>();
	public HashMap<String, ClassMemberDetails> newMethodNames = new HashMap<String, ClassMemberDetails>();

	public ClassDetails(String oldName, String newName) {
		this.oldName = oldName;
		this.newName = newName;
	}
}
