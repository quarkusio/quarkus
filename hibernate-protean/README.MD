# Hibernate ORM for Protean

Experiments to run Hibernate ORM on:
 - SubstrateVM
 - traditional JVM with Graal
 - experiments with Class Data Sharing
 - experiments with Jigsaw modules
 
The main focus is on `SubstrateVM`, each of these have a different bash script.

For Protean you can ignore the other scripts; focus on `graal-build-native.sh`.
 
## Dependencies

Some custom builds are needed as dependencies.

These will automatically be downloaded from the Protean Nexus, provided you have your credentials configured in your `~/.m2/settings.xml`: check your mail.

# Test on SubstrateVM

Now you can invoke the script from the checkout folder of this project:

      > ./graal-build-native.sh

 - Requires Graal on PATH and on JAVA_HOME, et al.
 - requires a recent Maven version

## Status

It should be able to successfully complete the `native-image` creation process.

Actually booting the resulting binary will fail; work in progress!