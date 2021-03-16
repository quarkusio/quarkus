package io.quarkus.qute.resteasy.deployment;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;

@CheckedTemplate
public class Templates {
    public static native TemplateInstance toplevel(String name);
}
