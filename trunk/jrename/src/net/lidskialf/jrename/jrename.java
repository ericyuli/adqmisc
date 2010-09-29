package net.lidskialf.jrename;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class jrename {
	private static ClassProcessor cp;
	private static String outBaseDir;
	private static ZipOutputStream zipOutStream = null;
	
	public static void main(String[] args) {
		
		if (args.length != 3) {
			System.err.println("Syntax: jrename <input> <output> [!]<database>\nWhere:");
			System.err.println("\t<input> may be a .class, .zip, .jar file, or a directory.");
			System.err.println("\t<output> may be a .zip, .jar file, or a directory.");
			System.err.println("\t<database> is the file to load/store the rename database from/to. Prefix with a '!' character to reverse the transformation.");
			System.exit(1);
		}
		
		String inFilename = args[0];
		String outFilename = args[1];
		String dbFilename = args[2];

		try {
			cp = new ClassProcessor(dbFilename);

			File inFile = new File(inFilename);		
			if (!inFile.exists()) {
				System.err.println("Failed to open file: " + inFilename);
				System.exit(1);
			}
			
			File outFile = new File(outFilename);
			if (outFilename.endsWith(".jar") || outFilename.endsWith(".zip") || outFile.isFile())
				zipOutStream = new ZipOutputStream(new FileOutputStream(outFile));
			else
				outBaseDir = outFilename;
			
			if (inFile.isDirectory()) {
				ProcessDirectory(inFile);
			} else if (inFilename.endsWith(".class")) {
				ProcessFile(inFile);
			} else if (inFilename.endsWith(".jar") || inFilename.endsWith(".zip")) {
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
							ProcessClass(tmp);
						} else {
							SaveData(tmp, entry.getName());
						}
					}				
				}
				zf.close();
			} else {
				System.err.println("I don't know what to do with: " + inFilename);
				System.exit(1);			
			}
			
			if (zipOutStream != null)
				zipOutStream.close();
			
			cp.SaveDatabase();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static void ProcessDirectory(File dir) throws IOException {
		for(File file: dir.listFiles()) {
			if (file.isDirectory()) 
				ProcessDirectory(file);
			else if (file.getName().endsWith(".class"))
				ProcessFile(file);
		}	
	}
	
	private static void ProcessFile(File inFile) throws IOException {		
		cp.ProcessFile(inFile);
		SaveData(cp.outData, cp.outClassName + ".class");
	}
	
	private static void ProcessClass(byte[] data) throws IOException {		
		cp.ProcessClass(data);
		SaveData(cp.outData, cp.outClassName + ".class");
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
