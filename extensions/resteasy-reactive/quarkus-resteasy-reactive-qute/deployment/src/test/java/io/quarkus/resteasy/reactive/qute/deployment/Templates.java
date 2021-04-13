package io.quarkus.resteasy.reactive.qute.deployment;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@CheckedTemplate
public class Templates {
    public static native TemplateInstance toplevel(String name);
}
