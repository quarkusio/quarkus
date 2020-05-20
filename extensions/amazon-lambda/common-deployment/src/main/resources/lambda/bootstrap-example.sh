#!/usr/bin/env bash

# Example wrapper bootstrap script for lambda custom runtime and quarkus

# Modify this file and rename it to 'bootstrap'. Put it within src/main/zip.native/ directory
# The quarkus lambda extension will automatically put the contents of this directory
# into your deployment zip

# Here's an example of setting up client SSL usage for S3 and other clients
# Make sure to copy Graal's libsunec.o and cacerts into src/main/zip.native/
#./runner -Djava.library.path=./ -Djavax.net.ssl.trustStore=./cacerts

./runner