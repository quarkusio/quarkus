package io.quarkus.it.freemarker;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.quarkus.freemarker.runtime.TemplatePath;

@Path("/freemarker")
public class FreemarkerTestResource {

    public static final String FOLDER_SUB_FTL = "folder/sub.ftl";

    public static final String OTHER_FTL = "other.ftl";

    public static final String EXTERNAL_EXTERNAL_FTL = "external/external.ftl";

    @Inject
    Configuration configuration;

    @Inject
    @TemplatePath(FOLDER_SUB_FTL)
    Template subTemplate;

    @Inject
    @TemplatePath(OTHER_FTL)
    Template otherTemplate;

    @Inject
    @TemplatePath("hello.ftl")
    Template hello;

    @GET
    @Path("/hello")
    @Produces(TEXT_PLAIN)
    public String hello(@QueryParam("name") String name) throws IOException, TemplateException {
        Map<String, String> model = new HashMap<>();
        model.put("name", name);
        StringWriter stringWriter = new StringWriter();
        hello.process(model, stringWriter);
        return stringWriter.toString();
    }

    @GET
    @Path("/hello_ftl")
    @Produces(TEXT_PLAIN)
    public String hello(@QueryParam("name") String name, @QueryParam("ftl") String ftl) throws IOException, TemplateException {
        Map<String, String> model = new HashMap<>();
        model.put("name", name);
        StringWriter stringWriter = new StringWriter();
        configuration.getTemplate(ftl).process(model, stringWriter);
        return stringWriter.toString();
    }

    @GET
    @Path("/sub")
    @Produces(TEXT_PLAIN)
    public String sub() throws IOException, TemplateException {
        return process(FOLDER_SUB_FTL, null);
    }

    @GET
    @Path("/subInject")
    @Produces(TEXT_PLAIN)
    public String subInject() throws IOException, TemplateException {
        return process(subTemplate, null);
    }

    @GET
    @Path("/person")
    @Produces(TEXT_PLAIN)
    public String person() throws IOException, TemplateException {
        return process(OTHER_FTL, createPersonModel());
    }

    @GET
    @Path("/personInject")
    @Produces(TEXT_PLAIN)
    public String personInject() throws IOException, TemplateException {
        return process(otherTemplate, createPersonModel());
    }

    @GET
    @Path("/personext")
    @Produces(TEXT_PLAIN)
    public String personext() throws IOException, TemplateException {
        return process(EXTERNAL_EXTERNAL_FTL, createPersonModel());
    }

    private String process(String templateName, Object model) throws IOException, TemplateException {
        return process(configuration.getTemplate(templateName), model);
    }

    private String process(Template template, Object model) throws TemplateException, IOException {
        StringWriter out = new StringWriter();
        template.process(model, out);
        return out.toString();
    }

    private Map<Object, Object> createPersonModel() {
        Map<Object, Object> model = new HashMap<>();
        model.put("person", new Person().setName("bob"));
        return model;
    }

}
