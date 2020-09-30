package io.quarkus.it.freemarker;

import static java.nio.charset.StandardCharsets.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Base64;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

public class Base64Directive implements TemplateDirectiveModel {
    @Override
    public void execute(Environment environment, Map map, TemplateModel[] templateModels, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        StringWriter sw = new StringWriter();
        body.render(sw);
        byte[] bytes = Base64.getEncoder().encode(sw.toString().getBytes(UTF_8));
        environment.getOut().write(new String(bytes, UTF_8));
    }
}
