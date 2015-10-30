Papaya-Builder
==============

A tool to build and customize Papaya.  The latest build of Papaya-Builder is located in the [Papaya](https://github.com/rii-mango/Papaya) project at libs/papaya-builder.jar.


How to build the build tool
-----
Run the Ant script, `build.xml`, found in the root of the project.  This will output `papaya-builder.jar` to the `build` 
folder.  Copy `papaya-builder.jar` to the `lib` folder in the [Papaya project](https://github.com/rii-mango/Papaya).

Usage
-----
```shell
usage: papaya-builder [options]
 -atlas <file>           add atlas (default atlas if no arg)
 -footnote <text>        add a footnote
 -help                   print this message
 -images <files>         images to include
 -local                  build for local usage
 -nodicom                do not include DICOM support
 -nojquery               do not include JQuery library
 -parameterfile <file>   specify parameters
 -root <dir>             papaya project directory
 -sample                 include sample image
 -singlefile             output a single HTML file
 -title <text>           add a title
```

###-atlas
Atlas must follow the [FSL Atlas Specification](http://ric.uthscsa.edu/mango/atlas_spec.html).  When building, 
provide the path to the atlas XML file. To use the default Talairach/MNI label atlas, leave the `<file>` field blank.

###-help
Prints the above list of parameters.

###-images
Specify one or more image file paths.  These images will appear as File menu options (similar to the sample image).

###-local
To build for local usage, include the `-local` flag.  In this case, image data is encoded and embedded within the 
JavaScript.

###-nodicom
Do not include the DICOM ([Daikon](https://github.com/rii-mango/Daikon)) library.

###-nojquery
Do not include the Jquery library.  If your webpage already loads Jquery, you can avoid adding it again to papaya.js. The output index.html will reference the ajax.googleapis.com hosted Jquery as a placeholder.

###-parameterfile
A file that contains the Papaya config parameters.  The contents of this file will be concatenated to "var params = " in the JavaScript portion of the output HTML header.  See http://rii.uthscsa.edu/mango/papaya_devguide.html for parameter usage.

###-root
Point the builder to the root of the papaya folder.  Omiting this option will use the current working directory.

###-sample
Use this option to include a sample image.  An _Add Sample Image_ option will appear in the Papaya viewer File menu.

###-singlefile
Outputs a single HTML file: collapses all HTML, CSS, JavaScript, and image data (if local) into one file.

###-title
Adds a title to the viewer.

###-footnote
Add a caption below the viewer.

Acknowledgments
-----
Papaya-Builder makes use of the following third-party libraries:
- [Apache Commons CLI](http://commons.apache.org/proper/commons-cli/)
- [Apache Commons Codec](http://commons.apache.org/proper/commons-codec/)
- [Apache Commons IO](Ihttp://commons.apache.org/proper/commons-io/)
- [JSON in Java](http://www.json.org/java/index.html)
- [YUICompressor](http://yui.github.io/yuicompressor/)
