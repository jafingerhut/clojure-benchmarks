#! /bin/bash

set -x

# Find all files beneath the current directory, except not directories
# themselves, and print a list of their relative path names to stdout:

# find . ! -type d

# Read a list of file names from stdin and invoke the perl command
# with all of those file names on its command line (because of xargs).

# The perl command with those options replaces any line beginning with
# the string "WARNING:" with a blank line, and writes the
# possibly-modified file back in place.

# Also replace any strings of the following form with nil, since the
# Clojure reader cannot handle them:
#   #<TransientVector clojure.lang.PersistentVector$TransientVector@191f1340>

# Put it all together:

find . ! -type d | xargs perl -pi -e 's/^WARNING:.*$//'
find . ! -type d | xargs perl -pi -e 's/#<TransientVector clojure.lang.PersistentVector\$TransientVector@[0-9a-fA-F]+>/nil/'


# How to confirm this did the desired thing:

# grep all files for lines containing WARNING:

#find . ! -type d | xargs grep -c WARNING:
