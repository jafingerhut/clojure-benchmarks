#! /bin/sh

# The use of 'tidy' to pretty-print the output was before I discovered
# XML::LibXML's $format argument to its toString() method.

#./xml-libxml-create-file.pl | tidy -q -i -xml > a.xml

./xml-libxml-create-file.pl > a.xml
