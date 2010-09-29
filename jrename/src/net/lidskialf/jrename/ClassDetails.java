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
	
	public ClassMemberDetails AddField(String oldFieldName, String newFieldName) {
		ClassMemberDetails cmd = new ClassMemberDetails(oldFieldName, newFieldName);
		oldFieldNames.put(oldFieldName, cmd);
		newFieldNames.put(newFieldName, cmd);
		return cmd;
	}
	
	public ClassMemberDetails AddMethod(String oldMethodName, String newMethodName, String returnDesc) {
		ClassMemberDetails cmd = new ClassMemberDetails(oldMethodName, newMethodName, returnDesc);
		oldMethodNames.put(oldMethodName, cmd);
		newMethodNames.put(newMethodName, cmd);
		return cmd;
	}
}
