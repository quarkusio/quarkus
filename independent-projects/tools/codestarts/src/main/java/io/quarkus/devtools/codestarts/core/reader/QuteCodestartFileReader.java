package io.quarkus.devtools.codestarts.core.reader;

import io.quarkus.devtools.codestarts.CodestartException;
import io.quarkus.devtools.codestarts.CodestartResource.Source;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Expression;
import io.quarkus.qute.ResultMapper;
import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.TemplateNode;
import io.quarkus.qute.Variant;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;

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

    @Override
    public Optional<String> read(Source source, String languageName, Map<String, Object> data)
            throws IOException {
        if (FilenameUtils.getName(source.path()).contains(INCLUDE_QUTE_FLAG)) {
            return Optional.empty();
        }
        return Optional.of(readQuteFile(source, languageName, data));
    }

    public static String readQuteFile(Source source, String languageName, Map<String, Object> data) {
        final String content = source.read();
        final Engine engine = Engine.builder().addDefaults()
                .addResultMapper(new MissingValueMapper())
                .removeStandaloneLines(true)
                .addLocator(
                        id -> findIncludeTemplate(source, languageName, id)
                                .map(IncludeTemplateLocation::new))
                .build();
        try {
            return engine.parse(content).render(data);
        } catch (TemplateException e) {
            throw new CodestartException("Error while rendering template: " + source.absolutePath(), e);
        }
    }

    private static Optional<Source> findIncludeTemplate(Source source, String languageName, String name) {
        final String includeFileName = name + INCLUDE_QUTE_FLAG;
        final Optional<Source> languageIncludeSource = source.getCodestartResource().getSource(languageName, includeFileName);
        if (languageIncludeSource.isPresent()) {
            return languageIncludeSource;
        }
        final Optional<Source> baseIncludeSource = source.getCodestartResource().getSource("base", includeFileName);
        if (baseIncludeSource.isPresent()) {
            return baseIncludeSource;
        }
        return Optional.empty();
    }

    private static class IncludeTemplateLocation implements TemplateLocator.TemplateLocation {
        private final Source source;

        public IncludeTemplateLocation(Source source) {
            this.source = source;
        }

        @Override
        public Reader read() {
            return new StringReader(source.read());
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
            if (expression.toOriginalString().equals("merged-content")) {
                return "{merged-content}";
            }
            throw new TemplateException("Missing required data: {" + expression.toOriginalString() + "}");
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
