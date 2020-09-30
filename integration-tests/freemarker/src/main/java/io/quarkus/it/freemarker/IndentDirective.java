package io.quarkus.it.freemarker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

public class IndentDirective implements TemplateDirectiveModel {

    @Override
    public void execute(Environment environment, Map params, TemplateModel[] templateModels, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        if (body != null) {
            int length = getIndentLength(params);
            StringWriter sw = new StringWriter();
            body.render(sw);
            environment.getOut().write(indent(sw.toString(), length));
        }
    }

    private String indent(String original, int length) {
        StringBuilder indentString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            indentString.append(" ");
        }
        StringWriter out = new StringWriter();
        PrintWriter pw = new PrintWriter(out);

        try (BufferedReader reader = new BufferedReader(new StringReader(original))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 0) {
                    pw.println(indentString + line);
                } else {
                    pw.println();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toString();
    }

    private int getIndentLength(Map params) {
        return params.containsKey("indent") ? Integer.parseInt(((SimpleScalar) params.get("indent")).getAsString()) : 0;
    }

}
