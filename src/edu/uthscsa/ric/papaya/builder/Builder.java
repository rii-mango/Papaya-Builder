
package edu.uthscsa.ric.papaya.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

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


public class Builder {

	private boolean useSample;
	private boolean useAtlas;
	private boolean isLocal;
	private boolean printHelp;
	private boolean useImages;
	private boolean singleFile;
	private boolean useParamFile;
	private boolean useTitle;
	private Options options;
	private File projectDir;
	private String buildVersion;
	private int buildNumber;

	public static final String ARG_SAMPLE = "sample";
	public static final String ARG_LOCAL = "local";
	public static final String ARG_HELP = "help";
	public static final String ARG_ROOT = "root";
	public static final String ARG_ATLAS = "atlas";
	public static final String ARG_IMAGE = "images";
	public static final String ARG_SINGLE = "singlefile";
	public static final String ARG_PARAM_FILE = "parameterfile";
	public static final String ARG_TITLE = "title";
	public static final String CLI_PROGRAM_NAME = "papaya-builder";
	public static final String OUTPUT_DIR = "build";
	public static final String OUTPUT_JS_FILENAME = "papaya.js";
	public static final String OUTPUT_CSS_FILENAME = "papaya.css";
	public static final String BUILD_PROP_FILE = "build.properties";
	public static final String BUILD_PROP_PAPAYA_VERSION_ID = "PAPAYA_VERSION_ID";
	public static final String BUILD_PROP_PAPAYA_BUILD_NUM = "PAPAYA_BUILD_NUM";
	public static final String CSS_BLOCK = "<!-- CSS GOES HERE -->";
	public static final String JS_BLOCK = "<!-- JS GOES HERE -->";
	public static final String PARAM_BLOCK = "<!-- PARAMS GO HERE -->";
	public static final String TITLE_BLOCK = "<!-- TITLE GOES HERE -->";
	public static final String PAPAYA_BLOCK = "<!-- PAPAYA GOES HERE -->";
	public static final String[] JS_FILES = { "lib/jquery.js", "src/js/constants.js", "src/js/utilities/base64-binary.js", "src/js/utilities/browser.js",
			"src/js/utilities/numerics.js", "src/js/utilities/pako-inflate.js", "src/js/utilities/platform.js", "src/js/utilities/utilities.js",
			"src/js/core/coordinate.js", "src/js/core/point.js", "src/js/volume/header.js", "src/js/volume/imagedata.js", "src/js/volume/imagedescription.js",
			"src/js/volume/imagedimensions.js", "src/js/volume/imagerange.js", "src/js/volume/imagetype.js", "src/js/volume/nifti/header-nifti.js",
			"src/js/volume/nifti/nifti.js", "src/js/volume/orientation.js", "src/js/volume/transform.js", "src/js/volume/volume.js",
			"src/js/volume/voxeldimensions.js", "src/js/volume/voxelvalue.js", "src/js/ui/dialog.js", "src/js/ui/menu.js", "src/js/ui/menuitem.js",
			"src/js/ui/menuitemcheckbox.js", "src/js/ui/menuitemfilechooser.js", "src/js/ui/menuitemrange.js", "src/js/ui/menuitemslider.js",
			"src/js/ui/menuitemspacer.js", "src/js/ui/toolbar.js", "src/js/viewer/atlas.js", "src/js/viewer/colortable.js", "src/js/viewer/display.js",
			"src/js/viewer/preferences.js", "src/js/viewer/screenslice.js", "src/js/viewer/screenvol.js", "src/js/viewer/viewer.js", "src/js/main.js",
			"src/js/license.js" };
	public static final String[] CSS_FILES = { "src/css/base.css", "src/css/ui/toolbar.css", "src/css/ui/menu.css", "src/css/ui/dialog.css",
			"src/css/utilities/nojs.css", "src/css/utilities/unsupported.css", "src/css/viewer/viewer.css" };
	public static final String RESOURCE_HTML = "index.html";
	public static final String SAMPLE_IMAGE_NII_FILE = "data/sample_image.nii.gz";
	public static final String SAMPLE_DEFAULT_ATLAS_FILE = "data/Talairach.xml";
	public static final String PAPAYA_LOADABLE_IMAGES = "papayaLoadableImages";
	public static final String DEFAULT_PARAMS = "var params = [];";



	public static void main(String[] args) {
		Builder builder = new Builder();

		// process command line
		CommandLine cli = builder.createCLI(args);
		builder.setUseSample(cli.hasOption(ARG_SAMPLE));
		builder.setUseAtlas(cli.hasOption(ARG_ATLAS));
		builder.setLocal(cli.hasOption(ARG_LOCAL));
		builder.setPrintHelp(cli.hasOption(ARG_HELP));
		builder.setUseImages(cli.hasOption(ARG_IMAGE));
		builder.setSingleFile(cli.hasOption(ARG_SINGLE));
		builder.setUseParamFile(cli.hasOption(ARG_PARAM_FILE));
		builder.setUseTitle(cli.hasOption(ARG_TITLE));

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

		// write JS
		File compressedFileJs = new File(outputDir, OUTPUT_JS_FILENAME);

		// build properties
		try {
			File buildFile = new File(builder.projectDir + "/" + BUILD_PROP_FILE);

			builder.readBuildProperties(buildFile);
			builder.buildNumber++; // increment build number
			builder.writeBuildProperties(compressedFileJs, true);
			builder.writeBuildProperties(buildFile, false);
		} catch (IOException ex) {
			System.err.println("Problem handling build properties.  Reason: " + ex.getMessage());
		}

		String htmlParameters = null;

		if (builder.isUseParamFile()) {
			String paramFileArg = cli.getOptionValue(ARG_PARAM_FILE);

			if (paramFileArg != null) {
				try {
					System.out.println("Including parameters...");

					String parameters = FileUtils.readFileToString(new File(paramFileArg), "UTF-8");
					htmlParameters = "var params = " + parameters + ";";
				} catch (IOException ex) {
					System.err.println("Problem reading parameters file! " + ex.getMessage());
				}
			}
		}

		String title = null;
		if (builder.isUseTitle()) {
			String str = cli.getOptionValue(ARG_TITLE);
			if (str != null) {
				str = str.trim();
				str = str.replace("\"", "");
				str = str.replace("'", "");

				if (str.length() > 0) {
					title = str;
					System.out.println("Using title: " + title);
				}
			}
		}

		try {
			JSONArray loadableImages = new JSONArray();

			// sample image
			if (builder.isUseSample()) {
				System.out.println("Including sample image...");

				File sampleFile = new File(builder.projectDir + "/" + SAMPLE_IMAGE_NII_FILE);
				String filename = Utilities.replaceNonAlphanumericCharacters(Utilities.removeNiftiExtensions(sampleFile.getName()));

				if (builder.isLocal()) {
					loadableImages.put(new JSONObject("{\"nicename\":\"Sample Image\",\"name\":\"" + filename + "\",\"encode\":\"" + filename + "\"}"));
					String sampleEncoded = Utilities.encodeImageFile(sampleFile);
					FileUtils.writeStringToFile(compressedFileJs, "var " + filename + "= \"" + sampleEncoded + "\";\n", "UTF-8", true);
				} else {
					loadableImages
							.put(new JSONObject("{\"nicename\":\"Sample Image\",\"name\":\"" + filename + "\",\"url\":\"" + SAMPLE_IMAGE_NII_FILE + "\"}"));
					FileUtils.copyFile(sampleFile, new File(outputDir + "/" + SAMPLE_IMAGE_NII_FILE));
				}
			}

			// atlas
			if (builder.isUseAtlas()) {
				Atlas atlas = null;

				try {
					String atlasArg = cli.getOptionValue(ARG_ATLAS);

					if (atlasArg == null) {
						atlasArg = (builder.projectDir + "/" + SAMPLE_DEFAULT_ATLAS_FILE);
					}

					File atlasXmlFile = new File(atlasArg);

					System.out.println("Including atlas " + atlasXmlFile);

					atlas = new Atlas(atlasXmlFile);
					File atlasJavaScriptFile = atlas.createAtlas(builder.isLocal());
					System.out.println("Using atlas image file " + atlas.getImageFile());

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

					builder.writeFile(atlasJavaScriptFile, compressedFileJs);
				} catch (IOException ex) {
					System.err.println("Problem finding atlas file.  Reason: " + ex.getMessage());
				}
			}

			// additional images
			if (builder.isUseImages()) {
				String[] imageArgs = cli.getOptionValues(ARG_IMAGE);

				if (imageArgs != null) {
					for (String imageArg : imageArgs) {
						File file = new File(imageArg);
						System.out.println("Including image " + file);

						String filename = Utilities.replaceNonAlphanumericCharacters(Utilities.removeNiftiExtensions(file.getName()));

						if (builder.isLocal()) {
							loadableImages.put(new JSONObject("{\"nicename\":\"" + Utilities.removeNiftiExtensions(file.getName()) + "\",\"name\":\""
									+ filename + "\",\"encode\":\"" + filename + "\"}"));
							String sampleEncoded = Utilities.encodeImageFile(file);
							FileUtils.writeStringToFile(compressedFileJs, "var " + filename + "= \"" + sampleEncoded + "\";\n", "UTF-8", true);
						} else {
							String filePath = "data/" + file.getName();
							loadableImages.put(new JSONObject("{\"nicename\":\"" + Utilities.removeNiftiExtensions(file.getName()) + "\",\"name\":\""
									+ filename + "\",\"url\":\"" + filePath + "\"}"));
							FileUtils.copyFile(file, new File(outputDir + "/" + filePath));
						}
					}
				}
			}

			File tempFileJs = null;

			try {
				tempFileJs = builder.createTempFile();
			} catch (IOException ex) {
				System.err.println("Problem creating temp write file.  Reason: " + ex.getMessage());
			}

			// write image refs
			FileUtils.writeStringToFile(tempFileJs, "var " + PAPAYA_LOADABLE_IMAGES + " = " + loadableImages.toString() + ";\n", "UTF-8", true);

			// compress JS
			tempFileJs = builder.concatenateFiles(JS_FILES, "js", tempFileJs);

			System.out.println("Compressing JavaScript... ");
			FileUtils.writeStringToFile(compressedFileJs, "\n", "UTF-8", true);
			builder.compressJavaScript(tempFileJs, compressedFileJs, new YuiCompressorOptions());
			//tempFileJs.deleteOnExit();
		} catch (IOException ex) {
			System.err.println("Problem concatenating JavaScript.  Reason: " + ex.getMessage());
		}

		// compress CSS
		File compressedFileCss = new File(outputDir, OUTPUT_CSS_FILENAME);

		try {
			File concatFile = builder.concatenateFiles(CSS_FILES, "css", null);
			System.out.println("Compressing CSS... ");
			builder.compressCSS(concatFile, compressedFileCss, new YuiCompressorOptions());
			concatFile.deleteOnExit();
		} catch (IOException ex) {
			System.err.println("Problem concatenating CSS.  Reason: " + ex.getMessage());
		}

		// write HTML
		try {
			System.out.println("Writing HTML... ");
			if (builder.singleFile) {
				builder.writeHtml(outputDir, compressedFileJs, compressedFileCss, htmlParameters, title);
			} else {
				builder.writeHtml(outputDir, htmlParameters, title);
			}
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
		options.addOption(new Option(ARG_SINGLE, "output a single HTML file"));
		options.addOption(new Option(ARG_HELP, "print this message"));
		options.addOption(OptionBuilder.withArgName("files").hasArgs().withDescription("images to include").create(ARG_IMAGE));
		options.addOption(OptionBuilder.withArgName("dir").hasArg().withDescription("papaya project directory").create(ARG_ROOT));
		options.addOption(OptionBuilder.withArgName("file").hasOptionalArg().withDescription("add atlas").create(ARG_ATLAS));
		options.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("specify parameters").create(ARG_PARAM_FILE));
		options.addOption(OptionBuilder.withArgName("text").hasArg().withDescription("add a title").create(ARG_TITLE));

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



	private File concatenateFiles(String[] files, String ext, File writeFile) throws IOException {
		String concat = "";

		for (String file2 : files) {
			File file = new File(projectDir + "/" + file2);
			concat += FileUtils.readFileToString(file, "UTF-8") + "\n";
		}

		if (writeFile == null) {
			writeFile = createTempFile();
		}

		FileUtils.writeStringToFile(writeFile, concat, "UTF-8", true);

		return writeFile;
	}



	private File writeFile(File readFile, File writeFile) throws IOException {
		if (writeFile == null) {
			writeFile = createTempFile();
		}

		String str = FileUtils.readFileToString(readFile, "UTF-8");
		FileUtils.writeStringToFile(writeFile, str, "UTF-8", true);

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



	private void writeHtml(File outputDir, String params, String title) throws IOException {
		File resourceOutputFile = new File(outputDir, RESOURCE_HTML);

		String str = Utilities.getResourceAsString(RESOURCE_HTML);
		str = replaceHtmlParamsBlock(str, params);
		str = replaceHtmlTitleBlock(str, title);
		str = replaceHtmlPapayaBlock(str);
		str = replaceHtmlCssBlock(str, null);
		str = replaceHtmlJsBlock(str, null);

		FileUtils.writeStringToFile(resourceOutputFile, str, "UTF-8");
	}



	private void writeHtml(File outputDir, File jsFile, File cssFile, String params, String title) throws IOException {
		File resourceOutputFile = new File(outputDir, RESOURCE_HTML);

		String html = Utilities.getResourceAsString(RESOURCE_HTML);
		String js = FileUtils.readFileToString(jsFile, "UTF-8");
		String css = FileUtils.readFileToString(cssFile, "UTF-8");

		html = replaceHtmlParamsBlock(html, params);
		html = replaceHtmlTitleBlock(html, title);
		html = replaceHtmlPapayaBlock(html);
		html = replaceHtmlCssBlock(html, css);
		html = replaceHtmlJsBlock(html, js);

		FileUtils.writeStringToFile(resourceOutputFile, html, "UTF-8");

		jsFile.delete();
		jsFile.deleteOnExit();

		cssFile.delete();
		cssFile.deleteOnExit();
	}



	private String replaceHtmlCssBlock(String html, String cssBlock) {
		String css = null;

		if (cssBlock != null) {
			css = "<style type=\"text/css\">\n" + cssBlock + "\n</style>\n";
		} else {
			css = "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + "papaya.css?version=" + buildVersion + "&build=" + buildNumber + "\" />";
		}

		return html.replace(CSS_BLOCK, css);
	}



	private String replaceHtmlJsBlock(String html, String jsBlock) {
		String js = null;

		if (jsBlock != null) {
			js = "<script type=\"text/javascript\">\n" + jsBlock + "\n</script>\n";
		} else {
			js = "<script type=\"text/javascript\" src=\"" + "papaya.js?version=" + buildVersion + "&build=" + buildNumber + "\"></script>";
		}

		return html.replace(JS_BLOCK, js);
	}



	private String replaceHtmlParamsBlock(String html, String params) {
		String js = null;

		if (params != null) {
			js = "<script type=\"text/javascript\">\n" + params + "\n</script>\n";
		} else {
			js = "<script type=\"text/javascript\">\n" + DEFAULT_PARAMS + "\n</script>\n";
		}

		return html.replace(PARAM_BLOCK, js);
	}



	private String replaceHtmlTitleBlock(String html, String titleStr) {
		String title = null;

		if (titleStr != null) {
			title = "<h3 style=\"text-align:center;font-family:sans-serif\">" + titleStr + "</h3>";
		} else {
			title = "";
		}

		return html.replace(TITLE_BLOCK, title);
	}



	private String replaceHtmlPapayaBlock(String html) {
		String papaya = null;

		if (isUseTitle()) {
			papaya = "<div style=\"width:100%;text-align:center;\"><div class=\"papaya\" data-params=\"params\"></div></div>";
		} else {
			papaya = "<div class=\"papaya\" data-params=\"params\"></div>";
		}

		return html.replace(PAPAYA_BLOCK, papaya);
	}



	private void writeBuildProperties(File file, boolean append) throws IOException {
		FileUtils.writeStringToFile(file, BUILD_PROP_PAPAYA_VERSION_ID + "=\"" + buildVersion + "\";\n", "UTF-8", append);
		FileUtils.writeStringToFile(file, BUILD_PROP_PAPAYA_BUILD_NUM + "=\"" + buildNumber + "\";\n", "UTF-8", true);
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



	// It's not a real Java properties file, so we need to handle reading it ourselves
	private void readBuildProperties(File file) throws IOException {
		List<String> lines = FileUtils.readLines(file);
		Iterator<String> it = lines.iterator();

		while (it.hasNext()) {
			String line = it.next();
			if (line.indexOf(BUILD_PROP_PAPAYA_VERSION_ID) != -1) {
				buildVersion = Utilities.findQuotedString(line);
			} else if (line.indexOf(BUILD_PROP_PAPAYA_BUILD_NUM) != -1) {
				buildNumber = Integer.parseInt(Utilities.findQuotedString(line));
			}
		}
	}



	public boolean isSingleFile() {
		return singleFile;
	}



	public void setSingleFile(boolean singleFile) {
		this.singleFile = singleFile;
	}



	public boolean isUseParamFile() {
		return useParamFile;
	}



	public void setUseParamFile(boolean useParamFile) {
		this.useParamFile = useParamFile;
	}



	public boolean isUseTitle() {
		return useTitle;
	}



	public void setUseTitle(boolean useTitle) {
		this.useTitle = useTitle;
	}
}
