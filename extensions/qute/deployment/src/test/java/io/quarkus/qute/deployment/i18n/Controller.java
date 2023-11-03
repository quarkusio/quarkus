package io.quarkus.qute.deployment.i18n;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

public class Controller {

    @CheckedTemplate
    static class Templates {

        static native TemplateInstance index(String name);

    }

    @MessageBundle
    public interface index {

        @Message("Hello {name}!")
        String hello(String name);
    }

}
