package net.lidskialf.jrename;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class ClassProcessor {
	
	public byte[] outData;
	public String outClassName;

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
		DeObfuscatorAdapter deob = new DeObfuscatorAdapter(writer);
		reader.accept(deob, 0);
		outData = writer.toByteArray();
		outClassName = deob.getClassNewFullName();
	}
}
