package net.lidskialf.jrename;

import java.io.*;
import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class dumpclass {
	public static void main(String[] args) {
		
		try {
			ClassReader reader = new ClassReader(new FileInputStream(args[0]));
			ClassWriter writer = new ClassWriter(0);
			TraceClassVisitor traceVisitor = new TraceClassVisitor(writer, new PrintWriter(System.out));
			reader.accept(traceVisitor, 0);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
