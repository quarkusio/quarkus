package io.quarkus.openapi.generator.deployment.template;

import java.io.IOException;
import java.util.Map;

import org.openapitools.codegen.api.AbstractTemplatingEngineAdapter;
import org.openapitools.codegen.api.TemplatingExecutor;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;

public class QuteTemplatingEngineAdapter extends AbstractTemplatingEngineAdapter {

    public static final String IDENTIFIER = "qute";
    public static final String[] INCLUDE_TEMPLATES = {
            "additionalEnumTypeAnnotations.qute",
            "additionalModelTypeAnnotations.qute",
            "beanValidation.qute",
            "beanValidationCore.qute",
            "beanValidationHeaderParams.qute",
            "bodyParams.qute",
            "enumClass.qute",
            "enumOuterClass.qute",
            "formParams.qute",
            "headerParams.qute",
            "pathParams.qute",
            "pojo.qute",
            "queryParams.qute"
    };
    public final Engine engine;

    public QuteTemplatingEngineAdapter() {
        this.engine = Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .removeStandaloneLines(true)
                .strictRendering(false)
                .build();
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String[] getFileExtensions() {
        return new String[] { IDENTIFIER };
    }

    @Override
    public String compileTemplate(TemplatingExecutor executor, Map<String, Object> bundle, String templateFile)
            throws IOException {
        this.cacheTemplates(executor);
        Template template = engine.getTemplate(templateFile);
        if (template == null) {
            template = engine.parse(executor.getFullTemplateContents(templateFile));
            engine.putTemplate(templateFile, template);
        }
        return template.data(bundle).render();
    }

    public void cacheTemplates(TemplatingExecutor executor) {
        for (String templateId : INCLUDE_TEMPLATES) {
            Template incTemplate = engine.getTemplate(templateId);
            if (incTemplate == null) {
                incTemplate = engine.parse(executor.getFullTemplateContents(templateId));
                engine.putTemplate(templateId, incTemplate);
            }
        }
    }
}
