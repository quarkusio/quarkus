# Neo4j example

## Running the tests

By default, the tests of this module are disabled.

To run the tests in a standard JVM with Neo4j started as a Docker container, you can run the following command:

```
mvn clean install -Dtest-neo4j -Ddocker
```

To also test as a native image, add `-Dnative`:

```
mvn clean install -Dtest-neo4j -Ddocker -Dnative
```

Alternatively you can connect to your own Neo4j instance or cluster.
Reconfigure the connection URL with `-Dneo4j.uri=bolt+routing://yourcluster:7687`;
you'll probably want to change the authentication password too: `-Dneo4j.password=NotS0Secret`.

The configuration file in our test is written in a way that it automatically sets Quarkus' configuration properties
from the environment, so you don't have to change the sources.


To open a connection to a different host or with different authentication from the generated Jar-Runner or binary,
pass on your details as system properties, i.e.:

```
./target/quarkus-integration-test-neo4j-999-SNAPSHOT-runner -Dquarkus.neo4j.uri=bolt://localhost:7687
```


## Native compilation

Neo4j's Bolt transport is encrypted by default.
That means, your image will need to have SSL native enabled.
Have a look at [Using SSL with Native Executables](https://quarkus.io/guides/native-and-ssl-guide) to understand the overhead of this.

The Quarkus maven plugin must be configured like this:

```
<plugin>
    <groupId>${project.groupId}</groupId>
    <artifactId>quarkus-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>native-image</id>
            <goals>
                <goal>native-image</goal>
            </goals>
            <configuration>
                <enableHttpUrlHandler>true</enableHttpUrlHandler>
                <enableHttpsUrlHandler>true</enableHttpsUrlHandler>
                <enableAllSecurityServices>true</enableAllSecurityServices>
            </configuration>
        </execution>
    </executions>
</plugin>
```

That's exactly the way the example project here is configured.

If you opt out of native SSL support by setting `quarkus.ssl.native=false`,
we disable encryption between the client and the Neo4j server which is *not* recommended.
