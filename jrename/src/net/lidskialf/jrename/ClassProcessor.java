package net.lidskialf.jrename;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.*;

public class ClassProcessor {
	
	public byte[] outData;
	public String outClassName;
	
	private static HashMap<String, Boolean> badNames = new HashMap<String, Boolean>();
	
	private static HashMap<String, ClassDetails> oldClassNames = new HashMap<String, ClassDetails>();
	private static HashMap<String, ClassDetails> newClassNames = new HashMap<String, ClassDetails>();

	static {		
		badNames.put("for", true);
		badNames.put("char", true);
		badNames.put("void", true);
		badNames.put("byte", true);
		badNames.put("do", true);
		badNames.put("int", true);
		badNames.put("long", true);
		badNames.put("else", true);
		badNames.put("case", true);
		badNames.put("new", true);
		badNames.put("goto", true);
		badNames.put("try", true);
		badNames.put("null", true);
	}
	
	public void ProcessFile(File inFile) throws FileNotFoundException, IOException {
		FileInputStream fis = new FileInputStream(inFile);
		byte[] inData = new byte[(int) inFile.length()];
		
		int pos = 0;
		while(true) {
			int len = fis.read(inData, pos, inData.length - pos);
			if (len <= 0)
				break;
			pos += len;
		}
		fis.close();
		
		ProcessClass(inData);
	}
	
	public void ProcessClass(byte[] inClass) {
		ClassReader reader = new ClassReader(inClass);
		ClassWriter writer = new ClassWriter(0);
		
//		TraceClassVisitor tcv = new TraceClassVisitor(writer, new PrintWriter(System.out));
		DeObfuscatorClassVisitor deob = new DeObfuscatorClassVisitor(this, writer);
		
		reader.accept(deob, 0);
		outData = writer.toByteArray();
		outClassName = deob.getClassNewFullName();
	}
	

	
	

	public String FixClassName(String classOldName) 
	{
		if (oldClassNames.containsKey(classOldName))
			return oldClassNames.get(classOldName).newName;

		String classNewName = classOldName;
		if (NeedsRenamed(classOldName)) {
			classNewName = GetClassPathName(classOldName) + "/class_" + GetClassLocalName(classOldName);
	
			String tmpClassName;
			for(int idx = 0; ; idx++) {
				tmpClassName = classNewName;
				if (idx > 0)
					tmpClassName += idx;
				
				if (newClassNames.containsKey(tmpClassName))
					continue;
				break;		
			}
			classNewName = tmpClassName;
		}

		ClassDetails classDetails = new ClassDetails(classOldName, classNewName);
		oldClassNames.put(classOldName, classDetails);
		newClassNames.put(classNewName, classDetails);
		return classNewName;
	}

	public String FixFieldName(String classOldName, String fieldOldName) 
	{
		String classNewName = FixClassName(classOldName);
		ClassDetails classDetails = oldClassNames.get(classOldName);
		String classNewLocalName = GetClassLocalName(classNewName);
		
		if (classDetails.oldFieldNames.containsKey(fieldOldName))
			return classDetails.oldFieldNames.get(fieldOldName).newName;

		String fieldNewName = fieldOldName;
		if (NeedsRenamed(fieldOldName)) {
			fieldNewName = "field_" + fieldOldName;
			
			String tmpFieldName;
			for(int idx = 0; ; idx++) {
				tmpFieldName = fieldNewName;
				if (idx > 0)
					tmpFieldName += idx;
				
				if (tmpFieldName.equals(classNewLocalName))
					continue;
				if (classDetails.newFieldNames.containsKey(tmpFieldName))
					continue;
				break;
			}
			fieldNewName = tmpFieldName;
		}
		
		ClassMemberDetails memberDetails = new ClassMemberDetails(fieldOldName, fieldNewName);
		classDetails.oldFieldNames.put(fieldOldName, memberDetails);
		classDetails.newFieldNames.put(fieldNewName, memberDetails);
		return fieldNewName;
	}

	public String FixMethodName(String classOldName, String methodOldName, String methodDesc) 
	{
		String methodReturnDesc = FixDescriptor(Type.getReturnType(methodDesc).getDescriptor());
		
		String classNewName = FixClassName(classOldName);
		ClassDetails classDetails = oldClassNames.get(classOldName);
		String classNewLocalName = GetClassLocalName(classNewName);
		
		if (classDetails.oldFieldNames.containsKey(methodReturnDesc + methodOldName))
			return classDetails.oldFieldNames.get(methodReturnDesc + methodOldName).newName;

		String methodNewName = methodOldName;
		if (NeedsRenamed(methodOldName)) {
			methodNewName = "method_" + methodOldName;
			
			String tmpMethodName;
			for(int idx = 0; ; idx++) {
				tmpMethodName = methodNewName;
				if (idx > 0)
					tmpMethodName += idx;
				
				if (tmpMethodName.equals(classNewLocalName))
					continue;

				if (classDetails.newMethodNames.containsKey(tmpMethodName)) {
					ClassMemberDetails tmp = classDetails.newMethodNames.get(tmpMethodName);
					if (tmp.returnDesc.equals(methodReturnDesc))
						return tmp.newName;
					continue;
				}
				break;
			}
			methodNewName = tmpMethodName;
		}
		
		ClassMemberDetails memberDetails = new ClassMemberDetails(methodOldName, methodNewName, methodReturnDesc);
		classDetails.oldMethodNames.put(methodOldName, memberDetails);
		classDetails.newMethodNames.put(methodNewName, memberDetails);
		return methodNewName;
	}
	
	public String FixMethodDescriptor(String desc) {
		Type returnType = Type.getType(FixDescriptor(Type.getReturnType(desc).getDescriptor()));
		StringBuffer returnDescSb = new StringBuffer();
		for(Type arg: Type.getArgumentTypes(desc)) {
			returnDescSb.append(FixDescriptor(arg.getDescriptor()));
		}
		return "(" + returnDescSb.toString() + ")" + returnType.toString();		
	}

	public String FixDescriptor(String desc)
	{
		Type fieldType = Type.getType(desc);
		switch(fieldType.getSort()) {
		case Type.OBJECT:
			return "L" + FixClassName(fieldType.getInternalName()) + ";";
		case Type.ARRAY:
			return "[" + FixDescriptor(fieldType.getElementType().getDescriptor());
		}
		return desc;
	}

	public String FixType(String internalName)
	{
		return FixType(internalName, false);
	}

	private String FixType(String internalName, boolean nested)
	{
		Type fieldType;
		try {
			fieldType = Type.getType(internalName);
		} catch (Exception ex) {
			fieldType = Type.getObjectType(internalName);
		}

		switch(fieldType.getSort()) {
		case Type.OBJECT:
			if (nested)
				return "L" + FixClassName(fieldType.getInternalName()) + ";";
			else
				return FixClassName(fieldType.getInternalName());

		case Type.ARRAY:
			return "[" + FixType(fieldType.getElementType().getDescriptor(), true);
		}
		return internalName;
	}

	
	public boolean NeedsRenamed(String testName)
	{
        testName = GetClassLocalName(testName);
        
        if (testName.charAt(0) == '<')
			return false;

        if (testName.length() > 0 && testName.length() <= 2)
			return true;

        if (testName.length() > 0 && testName.length() <= 3 && testName.contains("$"))
            return true;

        return badNames.containsKey(testName);
	}
	
    public static String GetClassLocalName(String fullName)
    {
        if (fullName.contains("/"))
            return fullName.substring(fullName.lastIndexOf('/') + 1);
        else 
            return fullName;
    }
	
    public static String GetClassPathName(String fullName)
    {
        if (fullName.contains("/"))
            return fullName.substring(0, fullName.lastIndexOf('/'));
        else 
            return "";
    }
}
