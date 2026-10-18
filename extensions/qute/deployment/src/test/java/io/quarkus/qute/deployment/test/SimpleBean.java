package io.quarkus.qute.deployment.test;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Singleton
public class SimpleBean {

    @Inject
    Template foo;

    public TemplateInstance fooInstance() {
        return foo.instance();
    }

}
