package io.quarkus.qute.resteasy.deployment;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@CheckedTemplate
public class Templates {
    public static native TemplateInstance toplevel(String name);
}
