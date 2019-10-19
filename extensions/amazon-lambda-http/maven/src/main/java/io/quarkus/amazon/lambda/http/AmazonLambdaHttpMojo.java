package io.quarkus.amazon.lambda.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Mojo(name = "configure-aws-lambda", requiresProject = false)
public class AmazonLambdaHttpMojo extends AbstractMojo {
    public static final String SAM_HANDLER = HttpHandler.class.getName() + "::handleRequest";
    public static final String SAM_RUNTIME = "java8";
    public static final String SAM_TEMPLATE = "quarkus.amazon-lambda-http.template";
    public static final String SAM_RESOURCE = "quarkus.amazon-lambda-http.resource-name";
    public static final String DEFAULT_TEMPLATE = "template.yaml";
    public static final String DEFAULT_RESOURCE = "Quarkus";

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(property = "sam.template", defaultValue = DEFAULT_TEMPLATE)
    private String templateFile;

    @Parameter(property = "sam.resource", defaultValue = DEFAULT_RESOURCE)
    private String resourceName;

    @SuppressWarnings("unchecked")
    @Override
    public void execute() {
        try {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Properties application = new Properties();
            File appProperties = new File("src/main/resources/application.properties");
            if (appProperties.exists()) {
                try (InputStream stream = new FileInputStream(appProperties)) {
                    application.load(stream);
                    templateFile = apply(application, templateFile, SAM_TEMPLATE, DEFAULT_TEMPLATE);

                    resourceName = apply(application, resourceName, SAM_RESOURCE, DEFAULT_RESOURCE);
                }
            }
            File configFile = new File(templateFile);

            Map template;
            Map<String, Object> resource;
            if (!configFile.exists()) {
                try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("template.yaml")) {
                    template = mapper.readValue(inputStream, LinkedHashMap.class);
                    Map<String, Object> resources = get(template, "Resources");

                    resource = get(resources, "Quarkus");
                    resources.remove("Quarkus");

                    resources.put(resourceName, resource);
                }
            } else {
                try (InputStream inputStream = new FileInputStream(configFile)) {
                    template = mapper.readValue(inputStream, LinkedHashMap.class);
                    Map<String, Object> resources = get(template, "Resources");

                    resource = get(resources, resourceName);

                    if (resource == null && resources.size() == 1) {
                        resource = (Map<String, Object>) resources.entrySet().iterator().next().getValue();
                    }
                }
            }
            if (resource != null) {
                Map<String, Object> properties = get(resource, "Properties");
                properties.put("Handler", SAM_HANDLER);
                properties.put("Runtime", SAM_RUNTIME);
                mapper.writer().withDefaultPrettyPrinter().writeValue(configFile, template);
            } else {
                throw new RuntimeException("Could not find the resource to update");
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String apply(Properties properties, String field, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value != null && field.equals(defaultValue)) {
            field = value;
        }
        return field;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(final Map template, final String... keys) {
        Map map = template;
        for (int i = 0; i < keys.length - 1; i++) {
            map = (Map) map.get(keys[i]);
            if (map == null) {
                throw new IllegalArgumentException("No object found with the key: " + keys[i]);
            }
        }
        return (T) map.get(keys[keys.length - 1]);
    }
}
