
package edu.uthscsa.ric.papaya.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;


public class Utilities {

	public static void delete(File file) throws IOException {

		if (file.isDirectory()) {
			if (file.list().length == 0) {
				file.delete();
			} else {
				String files[] = file.list();

				for (String temp : files) {
					File fileDelete = new File(file, temp);
					delete(fileDelete);
				}

				if (file.list().length == 0) {
					file.delete();
				}
			}
		} else {
			file.delete();
		}
	}



	public static void writeResourcetoFile(String resourceName, File file) throws IOException {
		InputStream is = Utilities.class.getResourceAsStream(resourceName);
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line = null;

		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}

		FileUtils.writeStringToFile(file, sb.toString());
	}



	public static String replaceNonAlphanumericCharacters(String str) {
		return str.replaceAll("[^A-Za-z0-9]", "_");
	}



	public static String removeNiftiExtensions(String str) {
		if (str.endsWith(".gz")) {
			str = str.substring(0, str.indexOf(".gz"));
		}

		if (str.endsWith(".nii")) {
			str = str.substring(0, str.indexOf(".nii"));
		}

		return str;
	}



	public static String encodeImageFile(File imageFile) throws IOException {
		Base64 base64 = new Base64();
		return new String(base64.encode(FileUtils.readFileToByteArray(imageFile)), "UTF-8");
	}
}
