package io.quarkus.devtools.generators.kinds.resource;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.quarkus.devtools.generators.Generator;
import io.quarkus.devtools.generators.file.FileGenerator;
import io.quarkus.devtools.generators.kinds.model.Model;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.StringWriter;

public class ResourceGenerator implements Generator {

    private static final String FOLDER_DESTINATION = "/resource";

    private final String template;
    private final FileGenerator fileGenerator;
    private final MavenProject mavenProject;

    public ResourceGenerator(String template, FileGenerator fileGenerator, MavenProject mavenProject) {
        this.template = template;
        this.fileGenerator = fileGenerator;
        this.mavenProject = mavenProject;
    }

    @Override
    public void generate(String params) {
        if (params.isEmpty()) {
            throw new IllegalArgumentException("params can not be null");
        }

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache m = mf.compile(this.template);

        Model modelFromParams = convertToModel(params);
        StringWriter stringWriter = new StringWriter();
        try {
            m.execute(stringWriter, modelFromParams).flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileGenerator.generateFile(modelFromParams, stringWriter.toString(), FOLDER_DESTINATION);
    }

    private Model convertToModel(String className) {
        return new Model(className.concat("Resource"), mavenProject.getGroupId(), className, className.toLowerCase());
    }
}
