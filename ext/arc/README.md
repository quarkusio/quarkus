# Weld Arc - "CDI lite" Experiments

This repo contains a working prototype of something that could be called "CDI lite".
The goal is to verify whether it's feasible to implement a "reasonable" subset of CDI features with minimal runtime.
The target envs are JVM, GraalVM (both jvm and aot modes).

**NOTE:** Jandex built from master is needed to build this project: https://github.com/wildfly/jandex

## Goals

* Instant startup and low memory footprint (compared to Weld)
* As little reflection in runtime as possible
* Acceptable build time increase
* Stick with CDI API and CDI idioms where possible

## Architecture

Currently, we generate metadata classes bytecode (aka "factory" classes) using [Gizmo](https://github.com/protean-project/shamrock/tree/master/gizmo).
One of the drawbacks is that debugging is not that simple.
On the other hand, the logic in generated classes should be quite straightforward and the developer can always decompile the classes.

## Features and Limitations

:heavy_check_mark: - basic support implemented
:white_check_mark: - not implemented yet
:x: - not supported, no plan to support ATM

* Programming model
  * :heavy_check_mark: Class beans
    * :heavy_check_mark: `@PostConstruct` and `@PreDestroy` callbacks
    * :white_check_mark: Lifecycle callbacks on superclasses
  * :heavy_check_mark: Producer methods and fields
    * :heavy_check_mark: Private members support
    * :white_check_mark: Disposers
  * :white_check_mark: Stereotypes
* Dependency injection
  * :heavy_check_mark: Field, constructor and initializer injection
    * :heavy_check_mark: Private injection fields
    * :heavy_check_mark: Private constructors
    * :white_check_mark: Private initializers
  * :heavy_check_mark: Type-safe resolution
    * :heavy_check_mark: Proper type-safe resolution rules at runtime; i.e. `ArcContainer.instance(Class<T>, Annotation...)`
* Scopes and Contexts:
  * :heavy_check_mark: `@Dependent`
  * :heavy_check_mark: `@Singleton`
  * :heavy_check_mark: `@RequestScoped`
  * :heavy_check_mark: `@ApplicationScoped`
  * :x: other built-in scopes such as `@SessionScoped`
  * :x: Custom scopes
* :heavy_check_mark: Client proxies
* Interceptors
  * :heavy_check_mark: `@AroundInvoke`
  * :heavy_check_mark: Lifecycle (`@PostConstruct`, `@PreDestroy`, `@AroundConstruct`)
  * :white_check_mark: Transitive interceptor bindings
  * :x: Interceptor methods on superclasses
* :white_check_mark: Events/observers
* :x: Decorators
* :x: Portable extensions
* :x: EL support
* :x: `BeanManager`
* :x: Specialization

## Modules

* `processor` - generates "bean" sources from a Jandex index
* `runtime` - classes needed at runtime
* `maven-plugin` - uses `processor` to generate sources and then compiles the sources
* `example` - a simple example; `target/generated-sources/java` contains the generated sources

## How to build

```bash
mvn clean install
```

## How to run the example

```bash
time java -jar example/target/arc-example-shaded.jar
```
And the results on my laptop:

```
real	0m0,133s
user	0m0,175s
sys	0m0,024s
```

### Native image

First build the image:

```bash
/opt/java/graalvm-ee-1.0.0-rc4/bin/native-image --verbose -jar target/arc-example-shaded.jar
```

Then run the example:

```bash
time ./arc-example-shaded
```

And the results on my laptop:

```
real	0m0,005s
user	0m0,000s
sys	0m0,005s
```
