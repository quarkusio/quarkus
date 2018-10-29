# ArC DI

This repo contains a working prototype of something that could be called "CDI lite".
The goal is to verify whether it's feasible to implement a "reasonable" subset of CDI features with minimal runtime overhead.
The target envs are JVM and GraalVM (both jvm and AOT compilation modes).

## Goals

* Support most commonly used CDI features
* Incorporate build-time optimizations
  * Instant startup
  * Low memory footprint
  * Negligible build time increase
* The impl should be SubstrateVM-friendly
  * As little reflection as possible
  * No dynamic class loading

## Features and Limitations

:heavy_check_mark: - basic support implemented
:white_check_mark: - not implemented yet
:x: - not supported, no plan to support ATM

* Programming model
  * :heavy_check_mark: Class beans
    * :heavy_check_mark: `@PostConstruct` and `@PreDestroy` callbacks
    * :heavy_check_mark: Lifecycle callbacks on superclasses
  * :heavy_check_mark: Producer methods and fields
    * :heavy_check_mark: Private members support
    * :heavy_check_mark: Disposers
  * :heavy_check_mark: Stereotypes
* Dependency injection
  * :heavy_check_mark: Field, constructor and initializer injection
    * :heavy_check_mark: Private injection fields
    * :heavy_check_mark: Private constructors
    * :heavy_check_mark: Private initializers
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
* :heavy_check_mark: Events/observers
* :x: Decorators
* :x: Portable extensions (runtime)
  * :white_check_mark: Build-time extensions
* :x: EL support
* :x: `BeanManager`
* :x: Specialization

## Modules

* `processor` - generates "bean metadata" from a Jandex index
* `runtime` - classes needed at runtime
* `maven-plugin` - uses `processor` to generate sources and then compiles the sources
* `example` - a simple example
