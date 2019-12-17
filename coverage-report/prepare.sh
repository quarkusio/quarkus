#!/bin/sh
rm -r target/classes src/main/java
mkdir -p  target/classes
mkdir -p  src/main/java

for j in '../extensions' '../core' '../devtools' '../independent-projects/'
do
    for i in `find $j -regex .*target/classes `
    do
        cp -r  $i/* target/classes/
    done
    for i in `find $j -regex .*src/main/java `
    do
        cp -r  $i/* src/main/java/
    done
done
#needed to make sure the script always suceeds
echo "complete"
