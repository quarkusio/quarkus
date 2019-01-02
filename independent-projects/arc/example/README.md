## How to build

```bash
mvn clean package
```

And then run (JVM mode):

```bash
time java -jar target/arc-example-shaded.jar
time java -jar target/weld-example-shaded.jar
```

## How to build native images

```bash
native-image --class-path target/arc-example-shaded.jar -H:Class=org.jboss.protean.arc.ArcMain -H:Name=arc-example
```

And for Weld:

```bash
native-image --class-path target/weld-example-shaded.jar -H:Class=org.jboss.protean.arc.WeldMain -H:Name=weld-example -H:+ReportUnsupportedElementsAtRuntime
```

Then run the example:

```bash
time ./arc-example
time ./weld-example
```