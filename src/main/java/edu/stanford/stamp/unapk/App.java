package edu.stanford.stamp.unapk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.dongliu.apk.parser.ApkParser;

/**
 * Quick tool to decompress an apk file, using https://github.com/xiaxiaocao/apk-parser
 * 
 * This tool will decode Android binary XML files into XML text files, but will 
 * not attempt to decompile or disassemble .dex files.
 */
public class App 
{

	private static void printUsage() {
		System.out.println("Usage: unapk my-app.apk [output-directory]");
	}
	
    public static void main( String[] args )
    {
    	if(args.length != 1 && args.length != 2) {
    		printUsage();
    		System.exit(1);
    	}
        String apkfilepath = args[0];
        String outdirpath = Paths.get(".").toAbsolutePath().normalize().toString();
        if(args.length >= 2) {
        	outdirpath = args[1];
        }
        File outdir = new File(outdirpath);
        if(outdir.exists()) {
        	if(!outdir.isDirectory()) {
        		System.out.println("Error: " + outdirpath + " already exists and is not a directory.");
        		System.exit(1);
        	}
        	if(outdir.list().length>0) {
        		// Non empty dir, let's create a subdirectory for the unpacked APK
        		outdirpath = new File(outdirpath,Paths.get(apkfilepath).getFileName().toString()).getPath();
        	}
        }
        outdir = new File(outdirpath); // Refresh in case we updated the path
        if(!outdir.exists()) {
        	outdir.mkdir();
        }
        System.out.println("Extracting APK file " + apkfilepath + " into " + outdirpath);
        
        byte[] buffer = new byte[1024];
        ZipInputStream zis = null;
        FileOutputStream fos;
        try {
        	ApkParser parser = new ApkParser(apkfilepath);
        	parser.setPreferredLocale(java.util.Locale.getDefault());
        	
        	// Extract AndroidManifest.xml
        	String manifestXml = parser.getManifestXml();
        	fos = new FileOutputStream(new File(outdirpath, "AndroidManifest.xml"));
        	(new PrintStream(fos)).print(manifestXml);
        	
        	zis = new ZipInputStream(new FileInputStream(apkfilepath));
			ZipEntry ze = zis.getNextEntry();
			while(ze!=null){
	 
				String fileName = ze.getName();
				File newFile = new File(outdirpath, fileName);
	 
				System.out.println("Extracting: "+ newFile.getAbsoluteFile());
	 
				//create all non exists folders
				//else you will hit FileNotFoundException for compressed folder
				new File(newFile.getParent()).mkdirs();
	 
		        boolean handledByAPKParser = false;
				if(fileName.endsWith(".xml")) {
					if(fileName.endsWith("AndroidManifest.xml")) {
		        		handledByAPKParser = true;
					} else if(fileName.startsWith("res")) {
						fos = new FileOutputStream(newFile);
						String resFile = parser.transBinaryXml(fileName);
						(new PrintStream(fos)).print(resFile);
						handledByAPKParser = true;
					}
				}
				if(!handledByAPKParser) {
					fos = new FileOutputStream(newFile);
				    int len;
				    while ((len = zis.read(buffer)) > 0) {
			   			fos.write(buffer, 0, len);
				    }
	 			}
		        fos.close();
		        ze = zis.getNextEntry();
			}
        	zis.closeEntry();
    		zis.close();
 		} catch(IOException ex) {
 			System.out.println("Error: IOException.");
 			ex.printStackTrace();
        	System.exit(1);
 		}
    }
}
