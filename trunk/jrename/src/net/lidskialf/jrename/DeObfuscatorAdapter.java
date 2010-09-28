package net.lidskialf.jrename;
import java.util.HashMap;
import java.util.Vector;

import org.objectweb.asm.*;

public class DeObfuscatorAdapter extends ClassAdapter {
	private static HashMap<String, Boolean> badNames = new HashMap<String, Boolean>();
	
	private static HashMap<String, ClassDetails> oldClassNames = new HashMap<String, ClassDetails>();
	private static HashMap<String, ClassDetails> newClassNames = new HashMap<String, ClassDetails>();

	private String classNewFullName;
	private String classOriginalFullName;
	
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
	
	public DeObfuscatorAdapter(ClassVisitor cv) {
		super(cv);
	}
	
	public String getClassNewFullName() {
		return classNewFullName;
	}

	@Override
	public void visit(int version, 
					  int access, 
					  String name, 
					  String signature,
					  String superName, 
					  String[] interfaces) {
		
		classOriginalFullName = name;
		
		name = FixClassName(name);
		classNewFullName = name;
		superName = FixClassName(superName);
		
		Vector<String> newInterfaces = new Vector<String>();
		for(String intf: interfaces) {
			newInterfaces.add(FixClassName(intf));
		}
		interfaces = newInterfaces.toArray(new String[newInterfaces.size()]);
				
		// FIXME
		if (signature != null)
			System.err.println("Signature was not null");
		// END FIXME		
		
		cv.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, 
								   String name, 
								   String desc,
								   String signature, 
								   Object value) {
		
		name = FixFieldName(classOriginalFullName, name);
		desc = FixDescriptor(desc);
		
		// FIXME
		if (signature != null)
			System.err.println("Signature was not null");
		// END FIXME
		
		return cv.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, 
									 String name, 
									 String desc,
									 String signature, 
									 String[] exceptions) {
		
		Type returnType = Type.getType(FixDescriptor(Type.getReturnType(desc).getDescriptor()));
		StringBuffer returnDescSb = new StringBuffer();
		for(Type arg: Type.getArgumentTypes(desc)) {
			returnDescSb.append(FixDescriptor(arg.getDescriptor()));
		}
		desc = "(" + returnDescSb.toString() + ")" + returnType.toString();
		
		name = FixMethodName(classOriginalFullName, name, returnType.getDescriptor(), returnDescSb.toString());

		if (exceptions != null) {
			Vector<String> newExceptions = new Vector<String>();
			for(String x: exceptions)
				newExceptions.add(FixClassName(x));
			exceptions = newExceptions.toArray(new String[newExceptions.size()]);
		}
		
		// FIXME
		if (signature != null)
			System.err.println("Signature was not null");
		// END FIXME
		
		return cv.visitMethod(access, name, desc, signature, exceptions);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		name = FixClassName(name);
		if (outerName != null)
			outerName = FixClassName(outerName);
		if (innerName != null)
			innerName = FixClassName(innerName);
		
		cv.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {

		// FIXME
		System.err.println("Outer class seen");
		// END FIXME
	}
	
	
	
	

	public String FixClassName(String classOldName) 
	{
		if (oldClassNames.containsKey(classOldName))
			return oldClassNames.get(classOldName).newName;

		String classNewName = classOldName;
		if (NeedsRenamed(classOldName))
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
		if (NeedsRenamed(fieldOldName))
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
		
		ClassMemberDetails memberDetails = new ClassMemberDetails(fieldOldName, fieldNewName);
		classDetails.oldFieldNames.put(fieldOldName, memberDetails);
		classDetails.newFieldNames.put(fieldNewName, memberDetails);
		return fieldNewName;
	}

	public String FixMethodName(String classOldName, String methodOldName, String methodReturnDesc, String methodArgsDesc) 
	{
		String classNewName = FixClassName(classOldName);
		ClassDetails classDetails = oldClassNames.get(classOldName);
		String classNewLocalName = GetClassLocalName(classNewName);
		
		if (classDetails.oldFieldNames.containsKey(methodReturnDesc + methodOldName))
			return classDetails.oldFieldNames.get(methodReturnDesc + methodOldName).newName;

		String methodNewName = methodOldName;
		if (NeedsRenamed(methodOldName))
			methodNewName = "method_" + methodOldName;
		
		String tmpMethodName;
		for(int idx = 0; ; idx++) {
			tmpMethodName = methodNewName;
			if (idx > 0)
				tmpMethodName += idx;
			
			if (tmpMethodName.equals(classNewLocalName))
				continue;
			if (classDetails.newMethodNames.containsKey(methodReturnDesc + tmpMethodName))
				continue;
			break;
		}
		methodNewName = tmpMethodName;
		
		ClassMemberDetails memberDetails = new ClassMemberDetails(methodOldName, methodNewName);
		classDetails.oldMethodNames.put(methodReturnDesc + methodOldName, memberDetails);
		classDetails.newMethodNames.put(methodReturnDesc + methodNewName, memberDetails);
		return methodNewName;
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
