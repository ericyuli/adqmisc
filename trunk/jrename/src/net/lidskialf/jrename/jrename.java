package net.lidskialf.jrename;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class jrename {
	private static ClassProcessor cp;
	private static String outBaseDir;
	private static ZipOutputStream zipOutStream = null;
	private static int phase = 1;
	
	public static void main(String[] args) {
		
		if (args.length != 2) {
			System.err.println("Syntax: jrename <input> <output>\nWhere:");
			System.err.println("\t<input> may be a .class, .zip, .jar file, or a directory.");
			System.err.println("\t<output> may be a .zip, .jar file, or a directory.");
			System.exit(1);
		}
		
		String inFilename = args[0];
		String outFilename = args[1];

		try {
			cp = new ClassProcessor();

			File outFile = new File(outFilename);
			if (outFilename.endsWith(".jar") || outFilename.endsWith(".zip") || outFile.isFile())
				zipOutStream = new ZipOutputStream(new FileOutputStream(outFile));
			else
				outBaseDir = outFilename;
			
			File inFile = new File(inFilename);		
			if (!inFile.exists()) {
				System.err.println("Failed to open file: " + inFilename);
				System.exit(1);
			}
			
			phase = 1;
			if (inFile.isDirectory()) {
				ProcessDirectory(inFile);
			} else if (inFilename.endsWith(".class")) {
				Process(inFile);
			} else if (inFilename.endsWith(".jar") || inFilename.endsWith(".zip")) {
				ProcessZip(inFile);
			} else {
				System.err.println("I don't know what to do with: " + inFilename);
				System.exit(1);			
			}
			
			phase = 2;
			cp.ProcessPhase2();

			phase = 3;
			if (inFile.isDirectory()) {
				ProcessDirectory(inFile);
			} else if (inFilename.endsWith(".class")) {
				Process(inFile);
			} else if (inFilename.endsWith(".jar") || inFilename.endsWith(".zip")) {
				ProcessZip(inFile);
			} else {
				System.err.println("I don't know what to do with: " + inFilename);
				System.exit(1);			
			}
			
			if (zipOutStream != null)
				zipOutStream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static void ProcessZip(File inFile) throws IOException {
		ZipFile zf = new ZipFile(inFile);
		Enumeration<? extends ZipEntry> entries = zf.entries();
		while(entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (!entry.isDirectory()) {
				byte[] tmp = new byte[(int)entry.getSize()];
				
				int pos = 0;
				InputStream is = zf.getInputStream(entry);
				while(true) {
					int len = is.read(tmp, pos, tmp.length - pos);
					if (len <= 0)
						break;
					pos += len;
				}
				is.close();
				
				if (entry.getName().endsWith(".class")) {
					Process(tmp);
				} else {
					if (phase == 3)
						SaveData(tmp, entry.getName());
				}
			}				
		}
		zf.close();		
	}
	
	private static void ProcessDirectory(File dir) throws IOException {
		for(File file: dir.listFiles()) {
			if (file.isDirectory()) 
				ProcessDirectory(file);
			else if (file.getName().endsWith(".class"))
				Process(file);
		}	
	}
	
	private static void Process(byte[] data) throws IOException {		
		switch(phase) {
		case 1:
			cp.ProcessPhase1(data);
			break;
		case 3:
			cp.ProcessPhase3(data);
			SaveData(cp.outData, cp.outClassName + ".class");
			break;
		}
	}	
	
	private static void Process(File inFile) throws IOException {		
		switch(phase) {
		case 1:
			cp.ProcessPhase1(inFile);
			break;
		case 3:
			cp.ProcessPhase3(inFile);
			SaveData(cp.outData, cp.outClassName + ".class");
			break;
		}
	}
	
	
	public static void SaveData(byte[] data, String filename) throws FileNotFoundException, IOException {
		if (zipOutStream != null) {
			ZipEntry ze = new ZipEntry(filename);
			zipOutStream.putNextEntry(ze);
			zipOutStream.write(data);
		} else {			
			File tmp = new File(filename);		
			File outputDir = new File(outBaseDir + File.separator + tmp.getParent());
			outputDir.mkdirs();
	
			FileOutputStream fos = new FileOutputStream(outBaseDir + File.separator + filename);
			fos.write(data);
			fos.close();
		}
	}
}
