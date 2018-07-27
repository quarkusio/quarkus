# Build

```bash
docker build -t protean/graalvm-native-image:latest .
```

# Run

```bash
docker run -it -v /path/to/protean/arc:/project --rm protean/graalvm-native-image -jar example/target/arc-example-shaded.jar
```
