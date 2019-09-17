# Devtools Specification

This document will attempt to specify the expected behavior of the various tools (æsh, maven, forge, etc.) developed to help developers onboard and manage their quarkus based work.  Much of this will presume maven based projects as gradle has not even come up in discussion as yet.  There are a few scenarios to cover:

1. Creating a new project
2. Updating an existing pom to support quarkus
    3. To a deployable project (a "jar" project)
    4. To a parent project (a "pom" project)
3. Adding extensions to an existing project
    4. To a pom that doesn't have quarkus support yet
    5. To a pom already configured for quarkus support

## The Initial Conditions
1. Path to project defaulting to the current folder (may not exist yet)
2. 7 values for the project
2. Optional list of additional extensions
3. Optional prefix name the names generated Resource and Application classes (default: Quarkus)
4. Optional package name
3. ???

## The Endgame
Regardless of the initial conditions, the final product should include:

1.  Dependencies on:
    2. The quarkus BOM in dependenciesManagement
    3. quarkus-jaxrs
    4. quarkus-junit5
5. The quarkus-maven-plugin
5. The `native` profile
6. A common property to track quarkus versions  (Debatable!)

## Target applications:

1. An æsh based command line
   * jar and native image based
1. A maven plugin
2. A forge plugin

## Creating a new project
Creating a project from scratch introduces the fewest barriers of course.  A basic template containing these is trivial to produce.  

## Updating an existing pom
Updating existing poms falls, generally, in to three categories:  1) adding initial quarkus support to a pristine pom, 2) running `create` on a pom already configured for quarkus, and 3) adding extensions to a pom that already supports quarkus.

### Initial quarkus support
In these cases, some of the initial conditions no longer apply.  Even though collected because the various interfaces will have them marked as required, the GAV values are now redundant.  These values are to be ignored in favor of the extant values found in the pom.

1. The pom should be updated to include the bom in the dependenciesManagement section.  One exception to this could be if a parent pom defines this bom then it could be skipped here.  Resolving that could be complicated and/or time consuming, however, so declaring it locally might still be the simpler solution especially as it's largely harmless
1. The quarkus-maven-plugin should be added to the plugins section of the build section.  Both sections should be created if they do not already exist.
1. A property should be created to store the quarkus version (named `quarkus.version`) for convenience.
1. References to the "core" quarkus extensions should be added to the dependencies section creating that section if necessary
2. Any extensions listed as one of the options should be added
3. None of these extensions should directly reference ${quarkus.version} but should rely on the bom definitions.

### Quarkus already configured
If the bom and the plugin have already been configured, the update process should terminate.  An argument could be made for checking the version and potentially updating it but this would be inappropriate.  Some versions might require more boilerplate and such an update might be unexpected and unwanted.

### Differing pom types
If the packaging type is "jar" work should proceed as described above.  If the packaging type is "pom," however, things change slightly.  In this case the process should do the following:

1. Add the bom to the dependencesManagement section as described above.
2. Add the plugin to the pluginManagement section creating any missing nesting sections as necessary.
3. It is unnecessary to add any dependencies on this level as that will be handled by updating the appropriate modules to use the quarkus plugin directly.

### Adding Extensions
Adding extensions should presume the existence of a bom and the plugin.  The added extensions should not explicitly declare a version.  If not bom is present in the dependencyManagement section, one should be added.

## Resolving conflicting definitions

1. GAV values passed in should always be discarded in favor of values in the pom.
2. When passing in a package name and a class names, if the class name is fully qualified, always use the package in the FQCN.  In the absence of a FQCN, use the specified package name given by the user.
3. Existing files are never to be overwritten. 
  * _Should alternative paths then be computed? simply log a warning?_
