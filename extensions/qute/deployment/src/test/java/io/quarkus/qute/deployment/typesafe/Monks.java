package io.quarkus.qute.deployment.typesafe;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;

public class Monks {

    @CheckedTemplate(basePath = "foo")
    static class Templates {

        static native TemplateInstance monk(String name);

    }

    @CheckedTemplate
    static class OtherTemplates {

        static native TemplateInstance monk(String name);

    }

}
