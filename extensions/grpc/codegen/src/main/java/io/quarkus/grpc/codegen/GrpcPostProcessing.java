package io.quarkus.grpc.codegen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.deployment.CodeGenContext;

public class GrpcPostProcessing {

    private static final Logger log = Logger.getLogger(GrpcPostProcessing.class);

    private static final String POST_PROCESS_QUARKUS_GENERATED_ANNOTATION = "quarkus.generate-code.grpc-post-processing.use-quarkus-generated-annotation";
    private static final String POST_PROCESS_NO_FINAL = "quarkus.generate-code.grpc-post-processing.no-final";
    // this is intentionally split so that it doesn't get replaced by the Jakarta transformer
    public static final String JAVAX_GENERATED = "javax" + ".annotation.Generated";
    public static final String QUARKUS_GENERATED = "io.quarkus.Generated";
    public static final String STUB = "Stub";
    public static final String BIND_METHOD = "bindService";

    private static final Pattern FINAL_STUB_CLASS = Pattern.compile("\\bfinal\\s+(class\\s+\\w*Stub\\b)");
    private static final Pattern FINAL_BIND_SERVICE = Pattern.compile(
            "\\bfinal\\s+(\\S+\\s+bindService\\s*\\()", Pattern.CASE_INSENSITIVE);

    private final Path root;
    private final boolean replaceGeneratedAnnotation;
    private final boolean removeFinal;

    public GrpcPostProcessing(CodeGenContext context, Path root) {
        this.root = root;
        this.replaceGeneratedAnnotation = isEnabled(context, POST_PROCESS_QUARKUS_GENERATED_ANNOTATION, true);
        this.removeFinal = isEnabled(context, POST_PROCESS_NO_FINAL, true);
    }

    public GrpcPostProcessing(Path root) {
        this.root = root;
        this.replaceGeneratedAnnotation = true;
        this.removeFinal = true;
    }

    /**
     * Methods used for the quarkus-grpc-stub project post-processing (as it's not a quarkus app)
     *
     * @param args expects the path to the source root of the files to post-process.
     */
    public static void main(String[] args) {
        for (String arg : args) {
            Path path = new File(arg).toPath();
            var postprocessing = new GrpcPostProcessing(path);
            postprocessing.postprocess();
        }

    }

    private boolean isEnabled(CodeGenContext context, String name, boolean def) {
        return Boolean.getBoolean(name) || context.config().getOptionalValue(name, Boolean.class).orElse(def);
    }

    public void postprocess() {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                    .filter(Files::isRegularFile)
                    .forEach(this::postprocessFile);
        } catch (IOException e) {
            log.error("Unable to walk the classes generated using protoc - skipping gRPC post processing", e);
        }
    }

    private void postprocessFile(Path file) {
        try {
            String content = Files.readString(file);
            String modified = content;

            if (replaceGeneratedAnnotation) {
                modified = modified.replace(JAVAX_GENERATED, QUARKUS_GENERATED);
            }

            if (removeFinal) {
                modified = FINAL_STUB_CLASS.matcher(modified).replaceAll("$1");
                modified = FINAL_BIND_SERVICE.matcher(modified).replaceAll("$1");
            }

            if (!modified.equals(content)) {
                Files.writeString(file, modified);
                log.debugf("Post-processed %s", file);
            }
        } catch (IOException e) {
            log.errorf("Failed to post-process %s: %s", file, e.getMessage());
        }
    }
}
