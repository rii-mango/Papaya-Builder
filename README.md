Papaya-Builder
==============

A tool to build and customize Papaya.  


How to build the build tool
-----
Run the Ant script, `build.xml`, found in the root of the project.  This will output `papaya-builder.jar` to the `build` 
folder.

Usage
-----
```shell
usage: papaya-builder [options]
  -atlas <file>   add atlas
  -help           print this message
  -local          build for local usage
  -root <dir>     papaya project directory
  -sample         include sample image
```

Atlases
-----
Atlas must follow the [FSL Atlas Specification](http://ric.uthscsa.edu/mango/imango_guide_atlas.html).  When building, 
provide the path to the atlas XML file.  Only non-probabilistic, label-based atlases are currently supported.  To use the 
default Talairach/MNI label atlas, leave the the `<file>` field blank.

Local
-----
To build for local usage, include the `-local` flag.  In this case, image data is Base64 encoded and embedded with the 
minimized JavaScript.

Root
-----
Point the builder to the root of the papaya folder.  Omiting this option will use the current working directory.

Sample
-----
Use this option to include a sample image.  An _Add Sample Image_ option will appear in the Papaya viewer File menu.
