# Infinispan Client for Quarkus

## Status

The RemoteCache by default only supports byte[] key and values. However a user can plug in a Marshaller to workaround
that. Please see the next few sections about features that currently work and those that don't

## Things that work

### Marshalling

Can be supplied via hotrod-client.properties file. The class must have a no arg public constructor. This
can also be configured at runtime by supplying DataFormat with the appropriate Marshaller instances on a key or value
basis. You can also add infinispan-remote-query-client as a dependency and configure the marshaller in the
hotrod.client-properties file to be `infinispan.client.hotrod.marshaller=org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller`
which allows support for protobuf marshalling. You can also provide `org.infinispan.protostream.MessageMarshaller` via @Produces
and we will automatically register those marshallers with the global ProtoStreamMarshaller.

### Protobuf Marshalling 

A user can define Injected protobuf Marshaller and FileDescriptorSource to configure the
ProtoStreamMarshaller if it is in use.

The code here would be part of the user application to allow protobuf marshalling for User types Book and Author.

```java
   @Produces
   MessageMarshaller bookMarshaller() {
      return new BookMarshaller();
   }

   @Produces
   MessageMarshaller authorMarshaller() {
      return new AuthorMarshaller();
   }

   @Produces
   FileDescriptorSource bookProtoDefinition() {
      return FileDescriptorSource.fromString("library.proto", "package book_sample;\n" +
            "\n" +
            "message Book {\n" +
            "  required string title = 1;\n" +
            "  required string description = 2;\n" +
            "  required int32 publicationYear = 3; // no native Date type available in Protobuf\n" +
            "\n" +
            "  repeated Author authors = 4;\n" +
            "}\n" +
            "\n" +
            "message Author {\n" +
            "  required string name = 1;\n" +
            "  required string surname = 2;\n" +
            "}");
   }
```

You may also now supply a file(s) in the META-INF directory of your project ending in .proto. Any such file will
be read and parsed as a protobuf file and automatically registered with the protostream marshaller if configured. Note
that the programmatic way via @Produces will overwrite any same named files (ie. library.proto).

### CDI

RemoteCache and RemoteCacheManager can both be injected via @Inject. The former allows named cache injection by
adding the Remote annotation, specifying the name of the cache. Currently the RemoteCacheManager is configured via the
hotrod-client.properties and/or microprofile-config.properties (with micro profile config replacing properties if both
are present).

### Near Caching

Bounded and Unbounded both work. Exception encountered when protobuf marshalling is enabled

### TLS/SSL

This is working, but requires some additional steps to get configured.

#### Configure truststore information

This is configured via the `quarkus.infinispan-client.trust-store` application property. Everything is the same as normal in that
you have to add the certificate from the server to the configured truststore if it already trusted in the default
java cacerts file.

#### Configuring keystore information

The keystore is configured via hotrod-client.properties file located in META-INF.

### Authentication

DIGEST_MD5, PLAIN, EXTERNAL were all tested to work.

### Querying

Querying is also supported. To be able to do DSL based queries you need to add the infinispan-query-dsl artifact to your
list of dependencies and then you can use it as normal. A simple example can be seen at
http://infinispan.org/docs/dev/user_guide/user_guide.html#a_remote_query_example

### Counters

Counters work :)

### Continuous Query

Continuous Query works

### Listeners

This is working

## Things to do

Annotation based protobuf marshalling is not yet currently supported. This requires additional changes as classes
must be generated at compile time and protostream, underlying library powering protobuf marshalling, only supports
runtime based class generation.

## Things to still verify work





### Multimap

Need to verify what is needed

### Transactions

This will need additional dependencies and verification
