# Contributing guide

**Want to contribute? Great!** 
We try to make it easy, and all contributions, even the smaller ones, are more than welcome.
This includes bug reports, fixes, documentation, examples... 
But first, read this page (including the small print at the end).

## Reporting an issue

This project uses Github issues to manage the issues. Open an issue directly in Github.

If you believe you found a bug, and it's likely possible, please indicate a way to reproduce it, what you are seeing and what you would expect to see.
Don't forget to indicate your Java, Maven and GraalVM version. 

## Before you contribute

To contribute, use Github Pull Requests, from your **own** fork.

### Code reviews

All submissions, including submissions by project members, need to be reviewed before being merged.

### Continuous Integration

Because we are all humans, the project use a continuous integration approach and each pull request triggers a full build.
Please make sure to monitor the output of the build and act accordingly.

### Tests and documentation are not optional

Don't forget to include tests in your pull requests. 
Also don't forget the documentation (reference documentation, javadoc...).

Be sure to test your pull request in:

1. Java mode
2. Native mode

## Setup   

If you have not done so on this machine, you need to:
 
* Install Git and configure your Github access
* Install Java SDK (OpenJDK recommended)
* Download and Apache Maven (3.5+) 
* Install [GraalVM](http://www.graalvm.org/downloads/) (community edition is enough)
* Install platform C developer tools:
    * Linux
        * Make sure headers are available on your system (you'll hit 'Basic header file missing (<zlib.h>)' error if they aren't).
            * On Fedora `sudo dnf install zlib-devel`
            * Otherwise `sudo apt-get install libz-dev`
    * macOS
        * `xcode-select --install` 
* Set `GRAALVM_HOME` to your GraalVM Home directory e.g. `/opt/graalvm` on Linux or `$location/JDK/GraalVM/Contents/Home` on macOS         
        
## Build

* Clone the repository
* Invoke `mvn clean install` from the root directory

## The small print

This project is an open source project, please act responsibly, be nice, polite and enjoy!
