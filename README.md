# Shamrock

Shamrock is framework that allows you process Java EE and Microprofile metadata at build time,
and use it to create low overhead jar files, as well as native images using Graal.


# Plugin Output

The shamrock build plugin generating wiring metadata for you application. The end result of this
is:

*   ${project.build.finalName}-runner.jar 
    The jar that contains all wiring metadata, as well as a manifest that correctly sets up the classpath. This jar can be executed directly using `java -jar`, or can be turned into a native image in the same manner.
     
*   ${project.build.finalName}.jar 
    The unmodified project jar, the shamrock plugin does not modify this.
    
*   lib/*
    A directory that contains all runtime dependencies. These are referenced by the `class-path` manifest entry in the runner jar.
    
        