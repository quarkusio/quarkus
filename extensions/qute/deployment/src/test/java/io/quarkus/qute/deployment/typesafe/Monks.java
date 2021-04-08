package io.quarkus.qute.deployment.typesafe;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

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
