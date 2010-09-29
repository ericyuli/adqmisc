package net.lidskialf.jrename;

import java.io.*;
import java.util.HashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

public class ClassProcessor {
	
	private String dbFilename;
	boolean swapOrder = false;
	
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
	
	public ClassProcessor(String dbFilename) throws FileNotFoundException, IOException {
		if (dbFilename.startsWith("!")) {
			swapOrder = true;
			dbFilename = dbFilename.substring(1);
		}
		this.dbFilename = dbFilename;
		
		File dbFile = new File(dbFilename);
		if (!dbFile.exists())
			return;
		
		BufferedReader br = new BufferedReader(new FileReader(dbFilename));
		String line = null;
		int lineno = 0;
		ClassDetails curClassDetails = null;
		while((line = br.readLine()) !=  null) {
			boolean isClass = !Character.isWhitespace(line.charAt(0));
			String[] bits = line.trim().split("\\s+");
			
			if (swapOrder) {
				String tmp = bits[0];
				bits[0] = bits[1];
				bits[1] = tmp;			
			}
			
			if (isClass) {
				if (bits.length != 2)
					throw new RuntimeException("Invalid class replace line @" + lineno);
				
				curClassDetails = new ClassDetails(bits[0], bits[1]);
				oldClassNames.put(curClassDetails.oldName, curClassDetails);
				newClassNames.put(curClassDetails.newName, curClassDetails);
			} else {
				if(curClassDetails == null)
					throw new RuntimeException("Class member line seen before class replace line @" + lineno);
				
				if (bits.length == 2) { /* field */
					ClassMemberDetails cmd = new ClassMemberDetails(bits[0], bits[1]);
					curClassDetails.oldFieldNames.put(cmd.oldName, cmd);
					curClassDetails.newFieldNames.put(cmd.newName, cmd);
				} else if (bits.length == 3) { /* method */
					ClassMemberDetails cmd = new ClassMemberDetails(bits[0], bits[1], bits[2]);
					curClassDetails.oldMethodNames.put(cmd.oldName, cmd);
					curClassDetails.newMethodNames.put(cmd.newName, cmd);					
				} else {
					throw new RuntimeException("Invalid class member line @" + lineno);					
				}				
			}
			
			lineno++;
		}
		
		br.close();
	}
	
	public void SaveDatabase() throws FileNotFoundException, IOException  {
		BufferedWriter bw = new BufferedWriter(new FileWriter(dbFilename));
		
		for(ClassDetails cd: oldClassNames.values()) {
			if (!swapOrder) {
				bw.write(cd.oldName);
				bw.write(" ");
				bw.write(cd.newName);
			} else {
				bw.write(cd.newName);
				bw.write(" ");
				bw.write(cd.oldName);
			}
			bw.newLine();
			
			for(ClassMemberDetails cmd: cd.oldFieldNames.values()) {
				bw.write("\t");
				if (!swapOrder) {
					bw.write(cmd.oldName);
					bw.write(" ");
					bw.write(cmd.newName);
				} else {
					bw.write(cmd.newName);
					bw.write(" ");
					bw.write(cmd.oldName);					
				}
				bw.newLine();
			}
			
			for(ClassMemberDetails cmd: cd.oldMethodNames.values()) {
				bw.write("\t");
				if (!swapOrder) {
					bw.write(cmd.oldName);
					bw.write(" ");
					bw.write(cmd.newName);
				} else {
					bw.write(cmd.newName);
					bw.write(" ");
					bw.write(cmd.oldName);					
				}
				bw.write(" ");
				bw.write(cmd.returnDesc);					
				bw.newLine();
			}
		}
		
		bw.flush();
		bw.close();
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

	public String FixFieldName(String classOldName, String fieldOldName, String desc) 
	{
		// FIXME: need to take descriptor into account
		
		
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

	public String FixMethodName(String classOldName, String methodOldName, String desc) 
	{
		// FIXME: need to take descriptor into account
		
		String methodReturnDesc = FixDescriptor(Type.getReturnType(desc).getDescriptor());
		
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
