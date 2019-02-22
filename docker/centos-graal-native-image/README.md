# Build

```bash
docker build -t quarkus/graalvm-native-image:latest .
```

# Run

```bash
docker run -it -v /path/to/quarkus/arc:/project --rm quarkus/graalvm-native-image -jar example/target/arc-example-shaded.jar
```
