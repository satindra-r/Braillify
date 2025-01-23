# Braillify
A simple command line tool to convert images to braille art made in technically 5 days for SDSLabs Makers'25
## Requirements
Requires java 8 or above
## Syntax
```
java -jar Braillify.jar -p <image path> [-o <out path>] [-d <width>,<height>] [-s <space character space/blank/dot>] [-i <invert Y/N>] [-e <edges Y/N>] [-c <colour Y/N>] [-m <mode min/rms/max/r/g/b>] [-b <brightness%>]
```
##Usage
change the brightness% from 0-100 and toggle invert to get desired parts of the image in the final render
change mode if brightness% does not work effectively
output to file if using on windows as cmd does not support braille by default
disable color if outputting to file as most text editors do not support ansi
change space character if there are font/rendering issues

