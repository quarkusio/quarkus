# Shamrock

[![Build Status](https://dev.azure.com/protean-ci/Shamrock/_apis/build/status/jbossas.protean-shamrock)](https://dev.azure.com/protean-ci/Shamrock/_build/latest?definitionId=4)

> Protean is a Cloud Native, Container First framework for writing Java applications.


* **Container First**: 
Minimal footprint Java applications optimal for running in containers
* **Cloud Native**:
Embraces 12 factor architecture in environments like Kubernetes.
* **Unify imperative and reactive**:
Brings under one programming model non blocking and imperative styles of development.
* **Standards-based**:
Based on the standards and frameworks you love and use (RESTEasy, Hibernate, Netty, Eclipse Vert.x, Apache Camel...)
* **Microservice First**:
Brings lightning fast startup time and code turn around to Java apps
* **Developer Joy**:
Development centric experience without compromise to bring your amazing apps to life in no time

_All under ONE framework._

## Getting Started

* [Documentation](http://10.8.247.58/nfs/protean/index.html)
* [Getting Started](http://10.8.247.58/nfs/protean/getting-started-guide.html)

---

## Shamrock

Shamrock, aka the core of Protean, is a framework that allows you to process Java EE and Eclipse MicroProfile metadata at build time,
and use it to create low overhead jar files, as well as native images using Graal/Substrate VM.

At the moment is has the following features:

- Clean build/runtime separation of components
- Bytecode recorders to allow for the generation of bytecode without knowledge of the class file format
- An API to easily enable reflection, resources, resource bundles and lazy clazz init support in Substrate
- Support for injection into build time processors
- Support for build and runtime config through MP config
- 'Instant Start' support on Graal through the use of static init to perform boot
- A lightweight CDI implementation called Arc
- A user friendly method for generating custom bytecode called Gizmo
- Various levels of support for:
    - JAX-RS (Resteasy)
    - Servlet (Undertow) 
    - CDI (Weld/Arc)
    - Microprofile Config (SmallRye)
    - Microprofile Health Check (SmallRye)
    - Microprofile OpenAPI (SmallRye)
    - Microprofile Metrics (SmallRye)
    - Microprofile Reactive Streams Operators (SmallRye)
    - Bean Validation (Hibernate Validator)
    - Transactions (Narayana)
    - Datasources (Agroal)
    - Eclipse Vert.x
- A Maven plugin to run the build, and create native images
- A JUnit runner that can run tests, and supports IDE usage
- A JUnit runner that can test a native image produced by the Maven plugin

### How to build Shamrock

* Install platform C developer tools:
    * Linux
        * Make sure headers are available on your system (you'll hit 'Basic header file missing (<zlib.h>)' error if they aren't).
            * On Fedora `sudo dnf install zlib-devel`
            * Otherwise `sudo apt-get install libz-dev`
    * macOS
        * `xcode-select --install`
* Install GraalVM (minimum RC10)
* Set `GRAALVM_HOME` to your GraalVM Home directory e.g. `/opt/graalvm` on Linux or `/Users/emmanuel/JDK/GraalVM/Contents/Home` on macOS
* `mvn install`

The default build will create two different native images, which is quite time consuming. You can skip this
by disabling the `native-image` profile: `mvn install -Dno-native`.

Wait. Success!

By default the build will use the native image server. This speeds up the build, but can cause problems due to the cache
not being invalidated correctly in some cases. To run a build with a new instance of the server you can use
`mvn install -Dnative-image.new-server=true`.

### Architecture Overview

Shamrock runs in two distinct phases. The first phase is build time processing, which is done by instances of ResourceProcessor:

https://github.com/protean-project/shamrock/blob/master/core/deployment/src/main/java/org/jboss/shamrock/deployment/ResourceProcessor.java

These processors run in priority order, in general they will read information from the Jandex index, and either directly output bytecode for
use at runtime, or provide information for later processors to write out.

These processors write out bytecode in the form of implementations of StartupTask:

https://github.com/protean-project/shamrock/blob/master/core/runtime/src/main/java/org/jboss/shamrock/runtime/StartupTask.java

When these tasks are created you can choose if you want them run from a static init method, or as part of the main() method execution.
This has no real effect when running on the JVM,
however when building a native image anything that runs from the static init method will be run at build time.
This is how Shamrock can provide instant start, as all deployment processing is done at image build time.
It is also why Weld can work without modification, as proxy generation etc is done at build time.

As part of the build process shamrock generates a main method that invokes all generated startup tasks in order.

### Runtime/deployment split

In general there will be two distinct artifacts, a runtime and a deployment time artifact. 

The runtime artifact should have a `dependencies.runtime` file in the root of the jar. This is a file that is produced
by the Maven dependencies plugin:

```
<plugin>
    <artifactId>Maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>resolve</goal>
            </goals>
            <phase>compile</phase>
            <configuration>
                <silent>true</silent>
                <outputFile>${project.build.outputDirectory}/dependencies.runtime</outputFile>
            </configuration>
        </execution>
    </executions>
</plugin>
```

This file tells the build plugin which dependencies are actually needed at runtime.

#### Packaging and dependencies

The deployment time artifact should have a dependency on the runtime artifact. When using Shamrock you just declare a
dependency on the deployment time artifacts for the features you want. This dependency *must* be scope `provided`.

The Shamrock plugin will not copy provided artifacts to the lib directory (and hence they will not be included in the
native image). The exception to this is artifacts that contain a `dependencies.runtime` files (as described above).

If this artifact has this file, or its Maven coordinates were listed in another artifact's file then it will be included
(this match does not take version into account, so if dependency resolution has resulted in a different version being
selected it will still be included).

This mechanism means that you only need to declare a single dependency per feature, but Shamrock still has enough information
to only include necessary runtime components.

### The Deployment Framework

The deployment framework allows you to process metadata from the application, and output resources (usually generated
bytecode) that will actually bootstrap the application at runtime. The idea of this is that all code related to
annotation/configuration parsing can run in a separate JVM, so the app can start faster and use less memory. 

To extend Shamrock you need to include a class that implements:

    org.jboss.shamrock.deployment.ShamrockSetup
    
This class can then be used to all implementation of the following:

    org.jboss.shamrock.deployment.ResourceProcessor
    org.jboss.shamrock.deployment.InjectionProvider
    
In addition to this it can be used to specify resources files that indicate that an archive should be processed
through the use of the `addApplicationArchiveMarker` call. Marker files are files such as `META-INF/beans.xml`
that indicate that a jar has application components and as such it should be indexed and handled by the processors.
By default jar files on the class path are not indexed unless they contain such a marker, or indexing has been specifically
configured.

#### ResourceProcessor and Bytecode recording

These startup tasks are generated by the bytecode recorder.
This recorder works by creating proxies of classes that contain deployment logic, and then recoding the invocations.

It will only be necessary to use this if you are integrating a framework that requires custom code to bootstrap it.
If you only need simple integrations like adding a bean or a Servlet then this is not necessary.
For example the health check integration requires no bytecode generation, as it just adds a servlet and some beans.
The integration basically just adds them to a list, and a later processor handles writing out the bytecode:

https://github.com/protean-project/shamrock/blob/master/health/deployment/src/main/java/org/jboss/shamrock/health/HealthProcessor.java

This is the Weld deployment time processor:

https://github.com/protean-project/shamrock/blob/master/weld/deployment/src/main/java/org/jboss/shamrock/weld/deployment/WeldAnnotationProcessor.java#L28

Using this runtime template:

https://github.com/protean-project/shamrock/blob/master/weld/runtime/src/main/java/org/jboss/shamrock/weld/runtime/WeldDeploymentTemplate.java

The first thing that happens is the processor getting a bytecode recorder by calling `addStaticInitTask()`.
The priority number that is passed in here controls the order in which the tasks are executed.
It then gets a copy of the template by calling `getRecordingProxy()`.

The next call to `createWeld()` will start recording bytecode.
The proxy will record the invocation and write it out to bytecode when the recorder is closed.
The return value of this method is also a proxy.
This proxy cannot be invoked on, but can be passed back into template methods,
and the recorder will automatically write out bytecode that does the same.

We see an example of this with the `template.addClass()` call, which adds a class to the deployment.
The first parameter is the proxy from the `createWeld()` call.
This method also shows how `Class` objects are handled.
Because this method takes a `Class` object,
and the actual classes are not loadable at build time we use the `classProxy()` method to create a `Class` object to pass in.

In general these invocations on the templates should look very similar to the same sequence of invocations you would actually make to start weld.

The next example is the Undertow processor, which is a bit more complex:

https://github.com/protean-project/shamrock/blob/master/undertow/deployment/src/main/java/org/jboss/shamrock/undertow/UndertowBuildStep.java#L90

https://github.com/protean-project/shamrock/blob/master/undertow/runtime/src/main/java/org/jboss/shamrock/undertow/runtime/UndertowDeploymentTemplate.java#L27

This example uses the `@ContextObject` annotation to pass parameters around, instead of relying on proxies.

If this annotation is applied to a method then the return value of that method will be placed in the `StartupContext` under the provided key name.
If the annotation is applied to a parameter then the value of that parameter will be looked up from the startup context.
This allows processors to interact with each other,
e.g. an early processor can create the `DeploymentInfo` and store it in the `StartupContext`,
and then a later processor can actually boot undertow.

In this case there are four processors, one that creates the `DeploymentInfo`,
another that actually adds all discovered Servlets, one that performs the actual deployment,
and one that actually starts undertow (which runs from the main method instead of static init).

The last of these also has an example of using MP config.
If you inject `ShamrockConfig` into your application the bytecode recorded will treat String's returned from config in a special manner.
If the recorder detects that a String has come from config then instead of just writing the value it will write some bytecode
that loads the value from MP config, and defaulting to the build time value if it is not present.
This means configuration can be applied at both build and runtime.

To pass to the StartupTask a list of class without loading the classes themselves, you can do the following

    org.jboss.shamrock.deployment.codegen.BytecodeRecorder#classProxy

to pass in class objects. It is a work around for the classes you need not being loadable from the processor.
You just pass in the class name, and it returns a Class object that is a proxy for the real Class.
There are examples in the Undertow one.

### Testing

#### JVM Based Testing

In order to support IDE usage Shamrock also has a 'runtime mode' that performs the build steps at runtime.
This will also be needed for Fakereplace support, to allow the new metadata to be computed at runtime.
This mode works by simply creating a special ClassLoader,
and writing all bytecode into an in memory map that this class loader can use to load the generated classes.
When running the tests this process is performed once, and then all tests are run against the resulting application.

The runner for this test is `org.jboss.shamrock.junit.ShamrockTest`.

These tests should be run by the Maven Surefire plugin, and as such should follow the standard surefire naming rules 
(\*TestCase). The application is started once at the beginning of the test suite run, and is shut down at the end. At
present there is no support for Arquillian style 'micro deployments', the whole application is under test.

Both the application and the test itself run in the same JVM, so in addition to integration testing over HTTP it is
also possible to directly test application components. 

#### Native Image Based Testing

To test the native image you can use the `org.jboss.shamrock.junit.SubstrateTest` runner.

These tests must be integration tests, as the image is generally built during the packaging phase.
As such they should follow the Maven Failsafe naming convention (\*ITCase). 

These tests work by simply booting the native image, and then allowing you to execute remote requests against it.
At present unit testing type functionality is supported (i.e. you cannot run test logic directly in the native image), 
although this will likely change. 

To run the tests from an IDE you will need to make sure the image has been built, and you may need to set
`-Dnative.image.path=full-path-to-image`. If this is not set Shamrock will try and guess the correct image location, by
assuming that the image name is `*-runner` and is located in the parent directory of the `test-classes` classpath entry.


### Reflection

In order to make reflection work Shamrock provides an API to easily register classes for reflection.
If you call `ProcessorContext.addReflectiveClass()` then a Graal AutoFeature will be written out that contains the bytecode to
register this class for reflection.

The reason why bytecode is used instead of JSON is that this does not require any arguments to the native-image command, it 'just works'.
It would also work better in a multiple jar scenario, as each jar could just have its own reflection wiring baked in.


### Plugin Output

The Shamrock build plugin generating wiring metadata for you application. The end result of this
is:

*   ${project.build.finalName}-runner.jar 
    The jar that contains all wiring metadata, as well as a manifest that correctly sets up the classpath.
    This jar can be executed directly using `java -jar`, or can be turned into a native image in the same manner.
     
*   ${project.build.finalName}.jar 
    The unmodified project jar, the Shamrock plugin does not modify this.
    
*   lib/\*
    A directory that contains all runtime dependencies. These are referenced by the `class-path` manifest entry in the runner jar.
    
### Shamrock Run Modes

Shamrock supports a few different run modes, to meet the various use cases. The core of how it works is the same in each
mode, however there are some differences. The two basic modes are:

*   Build Time Mode
    This mode involves building the wiring jar at build/provisioning time, and then executing the resulting output as a
    separate step. This mode is the basis for native image generation, as native images are generated from the output
    of this command. This is the only mode that is supported for production use.

*   Development Mode
    Runtime mode involves generating the wiring bytecode at startup time. This is useful when developing apps with Shamrock,
    as it allows you to test and run things without a full Maven. This is currently used for the JUnit test runner, 
    and for the `mvn compile shamrock:dev` command.

#### Indexing and Application Classes

Shamrock has the concept of 'application classes'. These are classes that should be indexed via jandex, and acted on
via deployment processors.

By default only the classes in the current application are indexed, however it is possible to include additional classes
through a few different methods.

The preferred way to index additional classes is to pre-generate the index using the Jandex Maven plugin. This will
create a serialized index in `META-INF/jandex.idx`, which will be loaded at augmentation time. This can be done
via the following configuration:

    <build>
      <plugins>
        <plugin>
          <groupId>org.jboss.jandex</groupId>
          <artifactId>jandex-maven-plugin</artifactId>
          <version>1.0.5</version>
          <executions>
            <execution>
              <id>make-index</id>
              <goals>
                <goal>jandex</goal>
              </goals>
              <!-- phase is 'process-classes by default' -->
              <configuration>
                <!-- Nothing needed here for simple cases -->
              </configuration>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>


In addition to this if an archive contains a `META-INF/beans.xml` file then it will also be indexed. 

It is also possible to force an artifact to be indexed via MicroProfile config:

    shamrock.index-dependency.somename.artifactId=common-jpa-entities
    shamrock.index-dependency.somename.groupId=org.jboss.shamrock
    
In this case the `somename` part of the config is an arbitrary name, and the artifact and group id's are the Maven artifact and group
ids. For now this will only work for artifacts that have a `META-INF/MANIFEST.MF` file, and that are layed out in a 
standard Maven repository structure. This means that if you have a multi-module Maven project this approach will currently
not work for dependencies that are part of the project itself, so in this case you should choose one of the other methods.
