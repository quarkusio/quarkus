package io.quarkus.devtools.codestarts.core.reader;

import io.quarkus.devtools.codestarts.CodestartException;
import io.quarkus.devtools.codestarts.CodestartResource;
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

    private static final String TPL_QUTE_FLAG = ".tpl.qute";
    private static final String ENTRY_QUTE_FLAG = ".entry.qute";
    public static final String INCLUDE_QUTE_FLAG = ".include.qute";

    @Override
    public boolean matches(String fileName) {
        return fileName.contains(TPL_QUTE_FLAG) || fileName.contains(ENTRY_QUTE_FLAG) || fileName.contains(INCLUDE_QUTE_FLAG);
    }

    @Override
    public String cleanFileName(String fileName) {
        return fileName.replaceAll(TPL_QUTE_FLAG, "").replace(ENTRY_QUTE_FLAG, "");
    }

    @Override
    public Optional<String> read(CodestartResource projectResource, Source source, String languageName,
            Map<String, Object> data)
            throws IOException {
        if (FilenameUtils.getName(source.path()).contains(INCLUDE_QUTE_FLAG)) {
            return Optional.empty();
        }
        return Optional.of(readQuteFile(projectResource, source, languageName, data));
    }

    public static String readQuteFile(CodestartResource projectResource, Source source, String languageName,
            Map<String, Object> data) {
        final String content = source.read();
        final String templateId = source.absolutePath();
        final Engine engine = Engine.builder().addDefaults()
                .addResultMapper(new MissingValueMapper())
                .removeStandaloneLines(true)
                // For now we need to disable strict rendering for codestarts
                // A param of an {#if} section is not considered falsy if the value represents a "not found" result
                // https://github.com/quarkusio/quarkus/pull/18227#discussion_r662301281
                .strictRendering(false)
                .addLocator(
                        id -> findIncludeTemplate(source.getCodestartResource(), languageName, id)
                                .map(IncludeTemplateLocation::new))
                .addLocator(
                        id -> findIncludeTemplate(projectResource, languageName, id)
                                .map(IncludeTemplateLocation::new))
                .addLocator(id -> Optional.of(new FallbackTemplateLocation()))
                .build();
        try {
            return engine.parse(content, null, templateId).render(data);
        } catch (Exception e) {
            throw new CodestartException("Error while rendering template: " + source.absolutePath(), e);
        }
    }

    private static Optional<Source> findIncludeTemplate(CodestartResource projectResource, String languageName, String name) {
        final String includeFileName = name + INCLUDE_QUTE_FLAG;
        final Optional<Source> languageIncludeSource = projectResource.getSource(languageName, includeFileName);
        if (languageIncludeSource.isPresent()) {
            return languageIncludeSource;
        }
        final Optional<Source> baseIncludeSource = projectResource.getSource("base", includeFileName);
        if (baseIncludeSource.isPresent()) {
            return baseIncludeSource;
        }
        return Optional.empty();
    }

    private static class FallbackTemplateLocation implements TemplateLocator.TemplateLocation {

        @Override
        public Reader read() {
            return new StringReader("");
        }

        @Override
        public Optional<Variant> getVariant() {
            return Optional.empty();
        }
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
            return Results.isNotFound(result);
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
