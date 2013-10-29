
package edu.uthscsa.ric.papaya.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;


public class Builder implements FilenameFilter {

	private boolean useSample;
	private boolean useAtlas;
	private boolean isLocal;
	private boolean printHelp;
	private boolean useImages;
	private Options options;
	private File projectDir;

	public static final String ARG_SAMPLE = "sample";
	public static final String ARG_LOCAL = "local";
	public static final String ARG_HELP = "help";
	public static final String ARG_ROOT = "root";
	public static final String ARG_ATLAS = "atlas";
	public static final String ARG_IMAGE = "images";
	public static final String CLI_PROGRAM_NAME = "papaya-builder";
	public static final String OUTPUT_DIR = "build";
	public static final String OUTPUT_JS_FILENAME = "papaya.js";
	public static final String OUTPUT_CSS_FILENAME = "papaya.css";
	public static final String BUILD_PROP_FILE = "build.properties";
	public static final String[] JS_DIRS = { "jquery/jquery.js", "classes/constants.js", "classes/utilities/", "classes/core/", "classes/volume/",
			"classes/volume/nifti/", "classes/viewer/", "classes/ui/", "classes/main.js" };
	public static final String[] CSS_DIRS = { "css/" };
	public static final String RESOURCE_HTML = "index.html";
	public static final String SAMPLE_IMAGE_NII_FILE = "data/sample_image.nii.gz";
	public static final String SAMPLE_DEFAULT_ATLAS_FILE = "data/Talairach.xml";
	public static final String PAPAYA_LOADABLE_IMAGES = "papayaLoadableImages";



	public static void main(String[] args) {
		Builder builder = new Builder();

		// process command line
		CommandLine cli = builder.createCLI(args);
		builder.setUseSample(cli.hasOption(ARG_SAMPLE));
		builder.setUseAtlas(cli.hasOption(ARG_ATLAS));
		builder.setLocal(cli.hasOption(ARG_LOCAL));
		builder.setPrintHelp(cli.hasOption(ARG_HELP));
		builder.setUseImages(cli.hasOption(ARG_IMAGE));

		// print help, if necessary
		if (builder.isPrintHelp()) {
			builder.printHelp();
			return;
		}

		// find project root directory
		if (cli.hasOption(ARG_ROOT)) {
			try {
				builder.projectDir = (new File(cli.getOptionValue(ARG_ROOT))).getCanonicalFile();
			} catch (IOException ex) {
				System.err.println("Problem finding root directory.  Reason: " + ex.getMessage());
			}
		}

		if (builder.projectDir == null) {
			builder.projectDir = new File(System.getProperty("user.dir"));
		}

		// clean output dir
		File outputDir = new File(builder.projectDir + "/" + OUTPUT_DIR);
		System.out.println("Cleaning output directory...");
		try {
			builder.cleanOutputDir(outputDir);
		} catch (IOException ex) {
			System.err.println("Problem cleaning build directory.  Reason: " + ex.getMessage());
		}

		if (builder.isLocal()) {
			System.out.println("Building for local usage...");
		}

		// compress JS
		try {
			File writeFile = builder.createTempFile();
			JSONArray loadableImages = new JSONArray();

			// write build properties
			File buildPropFile = new File(builder.projectDir + "/" + BUILD_PROP_FILE);
			builder.writeFile(buildPropFile, writeFile);

			// handle sample image
			if (builder.isUseSample()) {
				System.out.println("Including sample image...");

				File sampleFile = new File(builder.projectDir + "/" + SAMPLE_IMAGE_NII_FILE);
				String filename = Utilities.replaceNonAlphanumericCharacters(Utilities.removeNiftiExtensions(sampleFile.getName()));

				if (builder.isLocal()) {
					loadableImages.put(new JSONObject("{\"nicename\":\"Sample Image\",\"name\":\"" + filename + "\",\"encode\":\"" + filename + "\"}"));
					String sampleEncoded = Utilities.encodeImageFile(sampleFile);
					FileUtils.writeStringToFile(writeFile, "var " + filename + "= \"" + sampleEncoded + "\";", true);
				} else {
					loadableImages
							.put(new JSONObject("{\"nicename\":\"Sample Image\",\"name\":\"" + filename + "\",\"url\":\"" + SAMPLE_IMAGE_NII_FILE + "\"}"));
					FileUtils.copyFile(sampleFile, new File(outputDir + "/" + SAMPLE_IMAGE_NII_FILE));
				}
			}

			// handle atlas
			if (builder.isUseAtlas()) {
				Atlas atlas = null;

				try {
					String atlasArg = cli.getOptionValue(ARG_ATLAS);

					if (atlasArg == null) {
						atlasArg = SAMPLE_DEFAULT_ATLAS_FILE;
					}

					File atlasXmlFile = (new File(atlasArg)).getCanonicalFile();
					System.out.println("Including atlas " + atlasXmlFile);
					atlas = new Atlas(atlasXmlFile);
					File atlasJavaScriptFile = atlas.createAtlas(builder.isLocal());

					if (builder.isLocal()) {
						loadableImages.put(new JSONObject("{\"nicename\":\"Atlas\",\"name\":\"" + atlas.getImageFileNewName() + "\",\"encode\":\""
								+ atlas.getImageFileNewName() + "\",\"hide\":true}"));
					} else {
						File atlasImageFile = atlas.getImageFile();
						String atlasPath = "data/" + atlasImageFile.getName();

						loadableImages.put(new JSONObject("{\"nicename\":\"Atlas\",\"name\":\"" + atlas.getImageFileNewName() + "\",\"url\":\"" + atlasPath
								+ "\",\"hide\":true}"));
						FileUtils.copyFile(atlasImageFile, new File(outputDir + "/" + atlasPath));
					}

					builder.writeFile(atlasJavaScriptFile, writeFile);
				} catch (IOException ex) {
					System.err.println("Problem finding atlas file.  Reason: " + ex.getMessage());
				}
			}

			if (builder.isUseImages()) {
				String[] imageArgs = cli.getOptionValues(ARG_IMAGE);

				if (imageArgs != null) {
					for (int ctr = 0; ctr < imageArgs.length; ctr++) {
						File file = new File(imageArgs[ctr]);
						System.out.println("Including image " + file);

						String filename = Utilities.replaceNonAlphanumericCharacters(Utilities.removeNiftiExtensions(file.getName()));

						if (builder.isLocal()) {
							loadableImages.put(new JSONObject("{\"nicename\":\"" + Utilities.removeNiftiExtensions(file.getName()) + "\",\"name\":\""
									+ filename + "\",\"encode\":\"" + filename + "\"}"));
							String sampleEncoded = Utilities.encodeImageFile(file);
							FileUtils.writeStringToFile(writeFile, "var " + filename + "= \"" + sampleEncoded + "\";", true);
						} else {
							String filePath = "data/" + file.getName();
							loadableImages.put(new JSONObject("{\"nicename\":\"" + Utilities.removeNiftiExtensions(file.getName()) + "\",\"name\":\""
									+ filename + "\",\"url\":\"" + filePath + "\"}"));
							FileUtils.copyFile(file, new File(outputDir + "/" + filePath));
						}
					}
				}
			}

			// write image refs
			FileUtils.writeStringToFile(writeFile, "var " + PAPAYA_LOADABLE_IMAGES + " = " + loadableImages.toString() + ";", true);

			writeFile = builder.concatenateFiles(JS_DIRS, ".js", writeFile);
			File compressedFile = new File(outputDir, OUTPUT_JS_FILENAME);
			System.out.println("Compressing JavaScript... ");
			builder.compressJavaScript(writeFile, compressedFile, new YuiCompressorOptions());
			writeFile.deleteOnExit();
		} catch (IOException ex) {
			System.err.println("Problem concatenating JavaScript.  Reason: " + ex.getMessage());
		}

		// compress CSS
		try {
			File concatFile = builder.concatenateFiles(CSS_DIRS, ".css", null);
			File compressedFile = new File(outputDir, OUTPUT_CSS_FILENAME);
			System.out.println("Compressing CSS... ");
			builder.compressCSS(concatFile, compressedFile, new YuiCompressorOptions());
			concatFile.deleteOnExit();
		} catch (IOException ex) {
			System.err.println("Problem concatenating JavaScript.  Reason: " + ex.getMessage());
		}

		// write HTML
		try {
			System.out.println("Writing HTML... ");
			builder.writeHtml(outputDir);
		} catch (IOException ex) {
			System.err.println("Problem writing HTML.  Reason: " + ex.getMessage());
		}

		System.out.println("Done!  Output files located at " + outputDir);
	}



	@SuppressWarnings("static-access")
	private CommandLine createCLI(String[] args) {
		options = new Options();
		options.addOption(new Option(ARG_SAMPLE, "include sample image"));
		options.addOption(new Option(ARG_LOCAL, "build for local usage"));
		options.addOption(new Option(ARG_HELP, "print this message"));
		options.addOption(OptionBuilder.withArgName("files").hasArgs().withDescription("images to include").create(ARG_IMAGE));
		options.addOption(OptionBuilder.withArgName("dir").hasArg().withDescription("papaya project directory").create(ARG_ROOT));
		options.addOption(OptionBuilder.withArgName("file").hasOptionalArg().withDescription("add atlas").create(ARG_ATLAS));

		CommandLineParser parser = new BasicParser();
		CommandLine line = null;

		try {
			line = parser.parse(options, args, true);
		} catch (ParseException exp) {
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

		return line;
	}



	private void cleanOutputDir(File outputDir) throws IOException {
		Utilities.delete(outputDir);
		outputDir.mkdir();
	}



	private File concatenateFiles(String[] dirs, String ext, File writeFile) throws IOException {
		String concat = "";

		for (int ctr = 0; ctr < dirs.length; ctr++) {
			File path = new File(projectDir + "/" + dirs[ctr]);

			if (dirs[ctr].endsWith(ext)) {
				concat += FileUtils.readFileToString(path) + "\n";
			} else {
				File[] files = path.listFiles(this);
				for (int ctrF = 0; ctrF < files.length; ctrF++) {
					concat += FileUtils.readFileToString(files[ctrF]) + "\n";
				}
			}
		}

		if (writeFile == null) {
			writeFile = createTempFile();
		}

		FileUtils.writeStringToFile(writeFile, concat, true);

		return writeFile;
	}



	private File writeFile(File readFile, File writeFile) throws IOException {
		if (writeFile == null) {
			writeFile = createTempFile();
		}

		String str = FileUtils.readFileToString(readFile);
		FileUtils.writeStringToFile(writeFile, str, true);

		return writeFile;
	}



	private File createTempFile() throws IOException {
		return File.createTempFile(CLI_PROGRAM_NAME, null);
	}



	public void compressJavaScript(File inputFile, File outputFile, YuiCompressorOptions o) throws IOException {
		Reader in = null;
		Writer out = null;
		try {
			in = new InputStreamReader(new FileInputStream(inputFile), o.charset);

			JavaScriptCompressor compressor = new JavaScriptCompressor(in, new YuiCompressorErrorReporter());
			in.close();
			in = null;

			out = new OutputStreamWriter(new FileOutputStream(outputFile, true), o.charset);
			compressor.compress(out, o.lineBreakPos, o.munge, o.verbose, o.preserveAllSemiColons, o.disableOptimizations);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}



	public void compressCSS(File inputFile, File outputFile, YuiCompressorOptions o) throws IOException {
		Reader in = null;
		Writer out = null;
		try {
			in = new InputStreamReader(new FileInputStream(inputFile), o.charset);

			CssCompressor compressor = new CssCompressor(in);
			in.close();
			in = null;

			out = new OutputStreamWriter(new FileOutputStream(outputFile, true), o.charset);
			compressor.compress(out, o.lineBreakPos);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}



	private void writeHtml(File outputDir) throws IOException {
		Utilities.writeResourcetoFile(RESOURCE_HTML, new File(outputDir, RESOURCE_HTML));
	}



	private void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(CLI_PROGRAM_NAME + " [options]", options);
	}



	public boolean isLocal() {
		return isLocal;
	}



	public void setLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}



	public boolean isPrintHelp() {
		return printHelp;
	}



	public void setPrintHelp(boolean printHelp) {
		this.printHelp = printHelp;
	}



	@Override
	public boolean accept(File dir, String name) {
		return (name.endsWith(".js") || name.endsWith(".css"));
	}



	public boolean isUseSample() {
		return useSample;
	}



	public void setUseSample(boolean useSample) {
		this.useSample = useSample;
	}



	public boolean isUseAtlas() {
		return useAtlas;
	}



	public void setUseAtlas(boolean useAtlas) {
		this.useAtlas = useAtlas;
	}



	public boolean isUseImages() {
		return useImages;
	}



	public void setUseImages(boolean useImages) {
		this.useImages = useImages;
	}
}
