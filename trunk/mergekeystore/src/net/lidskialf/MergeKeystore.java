package net.lidskialf;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Enumeration;

public class MergeKeystore {

	public static final void main(String[] args) {
		
		if (args.length != 2) {
			System.err.println("Syntax: MergeKeystore <destination keystore> <keystore to merge in>");
			System.exit(0);
		}
		
		String destKeystoreFilename = args[0];
		String mergeKeystoreFilename = args[1];
		String destPassword = "password"; // args[2];
		String mergePassword = "password"; // args[3];
		
		try {
			// load the destination keystore
			KeyStore destKs = KeyStore.getInstance("JKS");
			try {
				FileInputStream in = new FileInputStream(destKeystoreFilename);
				destKs.load(in, destPassword.toCharArray());
				in.close();
			} catch (Exception ex2) {
				destKs = KeyStore.getInstance("JKS");
				destKs.load(null, destPassword.toCharArray());
			}

			// load the merge keystore
			KeyStore mergeKs = KeyStore.getInstance("JKS");
			FileInputStream in = new FileInputStream(mergeKeystoreFilename);
			mergeKs.load(in, mergePassword.toCharArray());
			in.close();
			
			// merge keys into the dest
			Enumeration aliases = mergeKs.aliases();
			while(aliases.hasMoreElements()) {
				String alias = (String) aliases.nextElement();
				Key key = mergeKs.getKey(alias, mergePassword.toCharArray());
				if (key != null) {
					java.security.cert.Certificate[] certs = mergeKs.getCertificateChain(alias);
					destKs.setKeyEntry(alias, key, destPassword.toCharArray(), certs);
				} else {
					Certificate cert = mergeKs.getCertificate(alias);
					destKs.setCertificateEntry(alias, cert);
				}
			}
			
			// save the destination keystore out again!
			FileOutputStream out = new FileOutputStream(destKeystoreFilename);
			destKs.store(out, destPassword.toCharArray());
			out.close();
			
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}		
	}
}
