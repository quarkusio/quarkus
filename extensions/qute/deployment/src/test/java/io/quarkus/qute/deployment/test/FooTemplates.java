package io.quarkus.qute.deployment.test;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@CheckedTemplate
public class FooTemplates {

    static native TemplateInstance foo(String name);

    static native TemplateInstance foo$bar();
}
