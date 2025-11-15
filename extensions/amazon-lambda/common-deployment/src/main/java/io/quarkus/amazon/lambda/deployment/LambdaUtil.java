package io.quarkus.amazon.lambda.deployment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

public class LambdaUtil {

    /**
     * Strips period, dash, and numbers. Turns characters after to uppercase. i.e.
     * Also strips "-SNAPSHOT" from end of name.
     *
     * "foo.bar-1.0-SNAPSHOT" to "FooBar"
     *
     * @param basename
     * @return
     */
    public static String artifactToLambda(String basename) {
        if (basename.endsWith("-SNAPSHOT"))
            basename = basename.substring(0, basename.length() - "-SNAPSHOT".length());
        String name = convertToken(basename, "[^a-zA-Z]");
        return name.trim();
    }

    protected static String convertToken(String basename, String token) {
        String[] splits = basename.split(token);
        if (splits == null || splits.length == 0)
            return basename;
        String name = "";
        for (String split : splits) {
            split = split.trim();
            if (split.isEmpty())
                continue;
            name = name + split.substring(0, 1).toUpperCase() + split.substring(1).toLowerCase();
        }
        return name;
    }

    public static void writeFile(OutputTargetBuildItem target, String name, String output) throws IOException {
        Path artifact = target.getOutputDirectory().resolve(name);
        String targetUri = target.getOutputDirectory().resolve("function.zip").toUri().toString().replace("file:", "fileb:");
        output = output.replace("${artifactId}", target.getBaseName())
                .replace("${buildDir}", target.getOutputDirectory().toString())
                .replace("${targetUri}", targetUri);
        Files.writeString(artifact, output);
    }

    public static void writeExecutableFile(OutputTargetBuildItem target, String name, String output) throws IOException {
        writeFile(target, name, output);

        Path artifact = target.getOutputDirectory().resolve(name);
        artifact.toFile().setExecutable(true, true);
    }

    public static String copyResource(String resource) throws Exception {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            byte[] byteArray = buffer.toByteArray();

            return new String(byteArray, StandardCharsets.UTF_8);
        }
    }

    public static void generateScripts(String handler, OutputTargetBuildItem target) throws Exception {
        String output = copyResource("lambda/bootstrap-example.sh");
        writeExecutableFile(target, "bootstrap-example.sh", output);

        String lambdaName = artifactToLambda(target.getBaseName());

        output = copyResource("lambda/manage.sh")
                .replace("${handler}", handler)
                .replace("${lambdaName}", lambdaName);
        writeExecutableFile(target, "manage.sh", output);

        output = copyResource("lambda/sam.jvm.yaml")
                .replace("${handler}", handler)
                .replace("${lambdaName}", lambdaName);
        writeFile(target, "sam.jvm.yaml", output);

        output = copyResource("lambda/sam.native.yaml")
                .replace("${lambdaName}", lambdaName);
        writeFile(target, "sam.native.yaml", output);
    }
}
