package net.lidskialf.jrename;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.*;

public class ClassProcessor {
	
	public byte[] outData;
	public String outClassName;
	
	private static HashMap<String, Boolean> badNames = new HashMap<String, Boolean>();
	
	private HashMap<String, ClassDetails> classes = new HashMap<String, ClassDetails>();
	private HashMap<String, String> classOldToNewNames = new HashMap<String, String>();
	
	private List<ClassReference> innerClasses = new ArrayList<ClassReference>();	
	private List<ClassReference> outerClasses = new ArrayList<ClassReference>();

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
	
	public void ProcessPhase1(File inFile) throws FileNotFoundException, IOException {
		ProcessPhase1(LoadFile(inFile));
	}
	
	public void ProcessPhase1(byte[] inClass) {
		ClassReader reader = new ClassReader(inClass);
		ClassWriter writer = new ClassWriter(0);		
		Phase1DeObfuscatorClassVisitor deob = new Phase1DeObfuscatorClassVisitor(this, writer);
		reader.accept(deob, 0);
	}
	
	public void ProcessPhase2() {
		/* first of all, figure out the new class name mappings */
		for(String oldClassFullName: classes.keySet()) {
			String classPath = GetClassPathName(oldClassFullName);
			String oldClassLocalName = GetClassLocalName(oldClassFullName);
			String newClassLocalName = oldClassLocalName;
			if (NeedsRenamed(oldClassLocalName)) {
				if (!classes.get(oldClassFullName).isInterface) {
					newClassLocalName = "intf_" + oldClassLocalName;
				} else {
					newClassLocalName = "class_" + oldClassLocalName;
				}				
			}
			String newClassFullName = classPath + "/" + newClassLocalName;
			
			classOldToNewNames.put(oldClassFullName, newClassFullName);				
		}

		/* next, see if we can recover any information from the innerclassnames of obfuscated inner classes */
		for(ClassReference innerClass: innerClasses) {
			if (!classes.containsKey(innerClass.toClassName))
				continue;
			if (!NeedsRenamed(innerClass.toClassName))
				continue;
			if (innerClass.innerClassName == null)
				continue;
			if (innerClass.fromClassName == null)
				continue;

			String newName = FixClassName(innerClass.fromClassName) + "$" + innerClass.innerClassName;
			classOldToNewNames.put(innerClass.toClassName, newName);
		}
		
		/* now, go through all loaded classes and sort out the names of fields and methods of overridden members only */
		for(String oldClassFullName: classes.keySet()) {
			ClassDetails cd = classes.get(oldClassFullName);
			
			for(ClassMemberDetails field: cd.fields) {
				if (cd.fieldOldToNewNameMapping.containsKey(field.name))
					continue;				
				RenameField(cd, field.name, true);
			}
			
			for(ClassMemberDetails method: cd.methods) {
				if (cd.methodOldToNewNameMapping.containsKey(method.name + "()" + method.desc))
					continue;
				RenameMethod(cd, method.name, method.desc, true);
			}
		}
		
		/* now, go through all loaded classes and sort out the names of fields and methods of non-overridden members */
		for(String oldClassFullName: classes.keySet()) {
			ClassDetails cd = classes.get(oldClassFullName);
			
			for(ClassMemberDetails field: cd.fields) {
				if (cd.fieldOldToNewNameMapping.containsKey(field.name))
					continue;				
				RenameField(cd, field.name, false);
			}
			
			for(ClassMemberDetails method: cd.methods) {
				if (cd.methodOldToNewNameMapping.containsKey(method.name + "()" + method.desc))
					continue;
				RenameMethod(cd, method.name, method.desc, false);
			}
		}
	}
	
	public void ProcessPhase3(File inFile) throws FileNotFoundException, IOException {
		ProcessPhase3(LoadFile(inFile));
	}
	
	public void ProcessPhase3(byte[] inClass) {
		ClassReader reader = new ClassReader(inClass);
		ClassWriter writer = new ClassWriter(0);		
		Phase3DeObfuscatorClassVisitor deob = new Phase3DeObfuscatorClassVisitor(this, writer);
		reader.accept(deob, 0);
		outData = writer.toByteArray();
		outClassName = deob.getClassNewName();
	}

	
	
	
	private String RenameMethod(ClassDetails cd, String methodOldName, String desc, boolean inheritedOnly) {
		// if its already been renamed in this class, just return that
		if (cd.methodOldToNewNameMapping.containsKey(methodOldName + "()" + desc))
			return cd.methodOldToNewNameMapping.get(methodOldName + "()" + desc);
		
		// if there is a superclass, traverse upwards checking if we've already renamed it
		if ((cd.superName != null) && classes.containsKey(cd.superName)) {	
			String methodNewName = RenameMethod(classes.get(cd.superName), methodOldName, desc, false);
			if (methodNewName != null)
				return SetMethodName(cd, methodOldName, methodNewName, desc);
		}
		
		// if there are interfaces, traverse upwards checking if we've already renamed it
		for(String intf: cd.interfaces) {
			if (!classes.containsKey(intf))
				continue;
			
			String methodNewName = RenameMethod(classes.get(intf), methodOldName, desc, false);
			if (methodNewName != null)
				return SetMethodName(cd, methodOldName, methodNewName, desc);
		}
		
		// return null now if we're not renaming inherited methods
		if (inheritedOnly)
			return null;
		
		// if a method needs renamed, all bets are off, and we need to ensure it has a unique name.
		// However, if it doesn't, we assume this is an unobfuscated method, so methods with the same name in the same class are ok
		// we could do more checking to ensure if the return types match in this case. If they don't then this shows
		// it actually /has/ been obfuscated, so we'd need to make 'em unique anyway. NeedsRenamed() would need 
		// to be updated to return true for these to cause 'em to be made unique.
		if (NeedsRenamed(methodOldName))
			return SetUniqueMethodName(cd, methodOldName, "method_" + methodOldName, desc);
		else
			return SetMethodName(cd, methodOldName, methodOldName, desc);
	}

	private String SetMethodName(ClassDetails cd, String methodOldName, String methodNewName, String desc) {
		cd.methodOldToNewNameMapping.put(methodOldName + "()" + desc, methodNewName);
		cd.methodNewNameUsed.put(methodNewName, true);
		
		return methodNewName;
	}
	
	private String SetUniqueMethodName(ClassDetails cd, String methodOldName, String methodNewName, String desc) {		
		int idx = 0;
		String methodUniqueNewName = methodNewName;
		while(true) {
			if (idx > 0)
				methodUniqueNewName = methodNewName + idx;
			if (!cd.methodNewNameUsed.containsKey(methodUniqueNewName)) {
				cd.methodOldToNewNameMapping.put(methodOldName + "()" + desc, methodUniqueNewName);
				cd.methodNewNameUsed.put(methodUniqueNewName, true);
				break;
			}
			
			idx++;
		}
		return methodUniqueNewName;
	}

	
	
	
	
	private String RenameField(ClassDetails cd, String fieldOldName, boolean inheritedOnly) {
		// if its already been renamed in this class, just return that
		if (cd.fieldOldToNewNameMapping.containsKey(fieldOldName))
			return cd.fieldOldToNewNameMapping.get(fieldOldName);
		
		// if there is a superclass, traverse upwards checking if we've already renamed it
		if ((cd.superName != null) && classes.containsKey(cd.superName)) {	
			String fieldNewName = RenameField(classes.get(cd.superName), fieldOldName, false);
			if (fieldNewName != null)
				return SetFieldName(cd, fieldOldName, fieldNewName);
		}
		
		// if there are interfaces, traverse upwards checking if we've already renamed it
		for(String intf: cd.interfaces) {
			if (!classes.containsKey(intf))
				continue;
			
			String fieldNewName = RenameField(classes.get(intf), fieldOldName, false);
			if (fieldNewName != null)
				return SetFieldName(cd, fieldOldName, fieldNewName);
		}
		
		// otherwise, rename it!
		if (inheritedOnly)
			return null;
		
		String fieldNewName = fieldOldName;
		if (NeedsRenamed(fieldOldName))
			fieldNewName = "field_" + fieldOldName;
		return SetUniqueFieldName(cd, fieldOldName, fieldNewName);
	}
	
	private String SetFieldName(ClassDetails cd, String fieldOldName, String fieldNewName) {
		if (cd.fieldOldToNewNameMapping.containsKey(fieldOldName))
			throw new RuntimeException("Duplicate field entry found: " + cd.name + " " + fieldOldName + " " + fieldNewName);
		cd.fieldOldToNewNameMapping.put(fieldOldName, fieldNewName);
		cd.fieldNewNameUsed.put(fieldNewName, true);
		
		return fieldNewName;
	}
	
	private String SetUniqueFieldName(ClassDetails cd, String fieldOldName, String fieldNewName) {
		int idx = 0;
		String fieldUniqueNewName = fieldNewName;
		while(true) {
			if (idx > 0)
				fieldUniqueNewName = fieldNewName + idx;
			
			if (!cd.fieldNewNameUsed.containsKey(fieldUniqueNewName)) {
				cd.fieldOldToNewNameMapping.put(fieldOldName, fieldUniqueNewName);
				cd.fieldNewNameUsed.put(fieldUniqueNewName, true);
				break;
			}
			
			idx++;
		}
		return fieldUniqueNewName;
	}
	
	
	
	
	
	
	
	
	public byte[] LoadFile(File inFile) throws FileNotFoundException, IOException {
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
		
		return inData;
	}
	
	public ClassDetails AddClass(String name, String superName, String[] interfaces, boolean isInterface) {		
		if (classes.containsKey(name))
			throw new RuntimeException("Duplicate class found: " + name);
		
		ClassDetails classDetails = new ClassDetails(name, superName, interfaces, isInterface);
		classes.put(name, classDetails);
		return classDetails;
	}
	
	public void AddInnerClassReference(String fromClassName, String toClassName, String innerClassName, boolean isInterface) {
		innerClasses.add(new ClassReference(fromClassName, toClassName, innerClassName, isInterface));
	}
	
	public void AddOuterClassReference(String fromClassName, String toClassName) {
		outerClasses.add(new ClassReference(fromClassName, toClassName, null, false));
	}

	public ClassMemberDetails AddField(String className, String fieldName, String fieldDesc, Object value) {
		ClassDetails classDetails = classes.get(className);
		if (classDetails == null)
			throw new RuntimeException("Attempt to add field to nonexistant class: " + className);
		
		return classDetails.AddField(fieldName, fieldDesc, value);
	}
	
	public ClassMemberDetails AddMethod(String className, String methodName, String desc) {
		ClassDetails classDetails = classes.get(className);
		if (classDetails == null)
			throw new RuntimeException("Attempt to add field to nonexistant class: " + className);
		
		return classDetails.AddMethod(methodName, GetMethodReturnDesc(desc), GetMethodArgsDesc(desc));
	}

	
	
	
	public String FixClassName(String classOldName) 
	{
		if (classOldToNewNames.containsKey(classOldName))
			return classOldToNewNames.get(classOldName);
		return classOldName;
	}
	
	public String FixFieldName(String classOldName, String fieldOldName) 
	{
		ClassDetails cd = classes.get(classOldName);
		if (cd == null)
			return fieldOldName;
		
		String fieldNewName = FixFieldName(cd, fieldOldName);
		if (fieldNewName == null)
			return fieldOldName;
		
		return fieldNewName;
	}
	
	private String FixFieldName(ClassDetails cd, String fieldOldName) 
	{
		if (cd.fieldOldToNewNameMapping.containsKey(fieldOldName))
			return cd.fieldOldToNewNameMapping.get(fieldOldName);
		
		if ((cd.superName != null) && classes.containsKey(cd.superName)) {
			String tmp = FixFieldName(classes.get(cd.superName), fieldOldName);
			if (tmp != null)
				return tmp;
		}
		
		for(String intf: cd.interfaces) {
			String tmp = FixFieldName(classes.get(intf), fieldOldName);
			if (tmp != null)
				return tmp;
		}
		
		return null;
	}

	public String FixMethodName(String classOldName, String methodOldName, String desc) 
	{
		ClassDetails cd = classes.get(classOldName);
		if (cd == null)
			return methodOldName;
		
		String methodNewName = FixMethodName(cd, methodOldName, desc);
		if (methodNewName == null)
			return methodOldName;
		
		return methodNewName;
	}
	
	private String FixMethodName(ClassDetails cd, String methodOldName, String desc)
	{	
		if (cd.methodOldToNewNameMapping.containsKey(methodOldName + "()" + desc))
			return cd.methodOldToNewNameMapping.get(methodOldName + "()" + desc);
		
		if ((cd.superName != null) && classes.containsKey(cd.superName)) {
			String tmp = FixMethodName(classes.get(cd.superName), methodOldName, desc);
			if (tmp != null)
				return tmp;
		}
		
		for(String intf: cd.interfaces) {
			if (!classes.containsKey(intf))
				continue;
			
			String tmp = FixMethodName(classes.get(intf), methodOldName, desc);
			if (tmp != null)
				return tmp;
		}
		
		return null;		
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
			StringBuilder sb = new StringBuilder();
			for(int i=0; i < fieldType.getDimensions(); i++)
				sb.append("[");
			sb.append(FixDescriptor(fieldType.getElementType().getDescriptor()));
			return sb.toString();
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
			StringBuilder sb = new StringBuilder();
			for(int i=0; i < fieldType.getDimensions(); i++)
				sb.append("[");
			sb.append(FixType(fieldType.getElementType().getDescriptor(), true));
			return sb.toString();
		}
		return internalName;
	}
	
	public String FixSignature(String signature) {
		if (signature == null)
			return null;
		
		SignatureReader reader = new SignatureReader(signature);
		SignatureWriter writer = new SignatureWriter();
		Phase3DeObfuscatorSignatureVisitor deob = new Phase3DeObfuscatorSignatureVisitor(this, writer);
		reader.accept(deob);
		return writer.toString();
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
	
    public String GetClassLocalName(String fullName)
    {
        if (fullName.contains("/"))
            return fullName.substring(fullName.lastIndexOf('/') + 1);
        else 
            return fullName;
    }
	
    public String GetClassPathName(String fullName)
    {
        if (fullName.contains("/"))
            return fullName.substring(0, fullName.lastIndexOf('/'));
        else 
            return "";
    }
    
    public String GetMethodReturnDesc(String desc)
    {
    	return Type.getReturnType(desc).getDescriptor();
    }
    
    public String GetMethodArgsDesc(String desc)
    {
		StringBuffer argsDescSb = new StringBuffer();
		for(Type arg: Type.getArgumentTypes(desc)) {
			argsDescSb.append(arg.getDescriptor());
		}
		return argsDescSb.toString();
    }
}
