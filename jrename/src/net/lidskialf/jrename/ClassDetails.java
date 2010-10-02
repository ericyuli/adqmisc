package net.lidskialf.jrename;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClassDetails {
	
	public String name;
	public String superName;
	public String[] interfaces;
	public boolean isInterface;
	
	public List<ClassMemberDetails> fields = new ArrayList<ClassMemberDetails>();
	public HashMap<String, String> fieldOldToNewNameMapping = new HashMap<String, String>();
	public HashMap<String, Boolean> fieldNewNameUsed = new HashMap<String, Boolean>();
	
	public List<ClassMemberDetails> methods = new ArrayList<ClassMemberDetails>();
	public HashMap<String, ClassMemberDetails> methodsLookup = new HashMap<String, ClassMemberDetails>();
	public HashMap<String, String> methodOldToNewNameMapping = new HashMap<String, String>();
	public HashMap<String, String> methodNewNameToReturnDescMapping = new HashMap<String, String>();

	public ClassDetails(String name, String superName, String[] interfaces, boolean isInterface) {
		this.name = name;
		this.superName = superName;
		this.interfaces = interfaces;
		this.isInterface = isInterface;
	}
	
	public ClassMemberDetails AddField(String fieldName, String fieldDesc) {
		ClassMemberDetails cmd = new ClassMemberDetails(fieldName, fieldDesc);
		fields.add(cmd);
		return cmd;
	}
	
	public ClassMemberDetails AddMethod(String methodName, String returnDesc, String argsDesc) {
		ClassMemberDetails cmd = new ClassMemberDetails(methodName, returnDesc, argsDesc);
		methods.add(cmd);
		methodsLookup.put(methodName + "!" + returnDesc + "!" + argsDesc, cmd);
		return cmd;
	}
}
