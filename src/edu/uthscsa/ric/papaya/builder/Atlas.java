
package edu.uthscsa.ric.papaya.builder;

import java.io.File;
import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;


public class Atlas {

	private final File xmlFile;
	private File imageFile;
	private JSONObject json;
	private String data;
	private boolean local;

	public static final String HEADER = "\"use strict\";var papaya = papaya || {};papaya.data = papaya.data || {};papaya.data.Atlas = papaya.data.Atlas || {};";
	public static final String NAME = "papaya.data.Atlas.name";
	public static final String LABELS = "papaya.data.Atlas.labels";
	public static final String DATA = "papaya.data.Atlas.data";
	public static final String IMAGE = "papaya.data.Atlas.image";



	public Atlas(File xmlFile) {
		this.xmlFile = xmlFile;
	}



	public File createAtlas(boolean local) throws IOException {
		this.local = local;

		convertToJson();
		findImageFile();

		if (local) {
			encodeImageFile();
		}

		return writeAtlasFile();
	}



	private void convertToJson() throws IOException {
		String xml = FileUtils.readFileToString(xmlFile);
		json = XML.toJSONObject(xml);

	}



	private void findImageFile() {
		String parentDir = xmlFile.getParent();

		JSONArray images = json.getJSONObject("atlas").getJSONObject("header").optJSONArray("images");
		if (images != null) {
			int numImages = images.length();
			long fileSize = Integer.MAX_VALUE;
			for (int ctr = 0; ctr < numImages; ctr++) {
				JSONObject image = images.getJSONObject(ctr);
				String filepath = image.getString("summaryimagefile");
				File file = new File(parentDir + "/" + filepath + ".nii.gz");
				long size = file.length();

				if (size < fileSize) { // find smallest file
					imageFile = file;
					fileSize = size;
				}
			}
		} else {
			String file = json.getJSONObject("atlas").getJSONObject("header").getJSONObject("images").getString("summaryimagefile");
			imageFile = new File(parentDir + "/" + file + ".nii.gz");
		}
	}



	private void encodeImageFile() throws IOException {
		Base64 base64 = new Base64();
		data = new String(base64.encode(FileUtils.readFileToByteArray(imageFile)), "UTF-8");
	}



	private File writeAtlasFile() throws IOException {
		File file = File.createTempFile("atlas", null);

		if (local) {
			FileUtils.writeStringToFile(file, HEADER + NAME + "=\"" + imageFile.getName() + "\";" + LABELS + "=" + json.toString() + ";" + DATA + "=\"" + data
					+ "\";");
		} else {
			FileUtils.writeStringToFile(file, HEADER + NAME + "=\"" + imageFile.getName() + "\";" + LABELS + "=" + json.toString() + ";" + IMAGE + "=\"data/"
					+ imageFile.getName() + "\";");
		}

		return file;
	}



	public File getImageFile() {
		return imageFile;
	}
}
