package io.quarkus.devtools.codestarts.reader;

import io.quarkus.devtools.codestarts.CodestartException;
import io.quarkus.qute.Engine;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Expression;
import io.quarkus.qute.ResultMapper;
import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.TemplateNode;
import io.quarkus.qute.Variant;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

final class QuteCodestartFileReader implements CodestartFileReader {

    private static final String FLAG = ".tpl.qute";
    public static final String INCLUDE_QUTE_FLAG = ".include.qute";

    @Override
    public boolean matches(String fileName) {
        return fileName.contains(FLAG) || fileName.contains(INCLUDE_QUTE_FLAG);
    }

    @Override
    public String cleanFileName(String fileName) {
        return fileName.replace(FLAG, "");
    }

    public Optional<String> read(Path sourceDirectory, Path relativeSourcePath, String languageName, Map<String, Object> data)
            throws IOException {
        if (relativeSourcePath.getFileName().toString().contains(INCLUDE_QUTE_FLAG)) {
            return Optional.empty();
        }
        return Optional.of(readQuteFile(sourceDirectory, relativeSourcePath, languageName, data));
    }

    public static String readQuteFile(Path sourceDirectory, Path relativeSourcePath, String languageName,
            Map<String, Object> data) throws IOException {
        final Path sourcePath = sourceDirectory.resolve(relativeSourcePath);
        final String content = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
        final Engine engine = Engine.builder().addDefaults()
                .addResultMapper(new MissingValueMapper())
                .removeStandaloneLines(true)
                .addLocator(id -> findIncludeTemplate(sourceDirectory, languageName, id).map(IncludeTemplateLocation::new))
                .build();
        try {
            return engine.parse(content).render(data);
        } catch (TemplateException e) {
            throw new IOException("Error while rendering template: " + sourcePath.toString(), e);
        }
    }

    private static Optional<Path> findIncludeTemplate(Path sourceDirectory, String languageName, String name) {
        final Path codestartPath = sourceDirectory.getParent();
        final String includeFileName = name + INCLUDE_QUTE_FLAG;
        final Path languageIncludeTemplate = codestartPath.resolve(languageName + "/" + includeFileName);
        if (Files.isRegularFile(languageIncludeTemplate)) {
            return Optional.of(languageIncludeTemplate);
        }
        final Path baseIncludeTemplate = codestartPath.resolve("base/" + includeFileName);
        if (Files.isRegularFile(baseIncludeTemplate)) {
            return Optional.of(baseIncludeTemplate);
        }
        return Optional.empty();
    }

    private static class IncludeTemplateLocation implements TemplateLocator.TemplateLocation {

        private final Path path;

        private IncludeTemplateLocation(Path path) {
            this.path = path;
        }

        @Override
        public Reader read() {
            try {
                return Files.newBufferedReader(path);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public Optional<Variant> getVariant() {
            return Optional.empty();
        }
    }

    static class MissingValueMapper implements ResultMapper {

        public boolean appliesTo(TemplateNode.Origin origin, Object result) {
            return Results.Result.NOT_FOUND.equals(result);
        }

        public String map(Object result, Expression expression) {
            throw new CodestartException("Missing required data: {" + expression.toOriginalString() + "}");
        }
    }

    /**
     * private static CompletionStage<Object> replaceResolveAsync(EvalContext context) {
     * String text = (String) context.getBase();
     * switch (context.getName()) {
     * case "replace":
     * if (context.getParams().size() == 2) {
     * return context.evaluate(context.getParams().get(0)).thenCombine(context.evaluate(context.getParams().get(1)),
     * (r1, r2) -> CompletableFuture.completedFuture(text.replace(r1.toString(), r2.toString())));
     * }
     * default:
     * return Results.NOT_FOUND;
     * }
     * }
     **/
}
