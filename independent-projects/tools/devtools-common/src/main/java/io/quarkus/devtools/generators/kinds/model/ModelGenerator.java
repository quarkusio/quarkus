package io.quarkus.devtools.generators.kinds.model;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.generators.Generator;
import io.quarkus.devtools.generators.file.FileGenerator;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.project.MavenProject;

public class ModelGenerator implements Generator {

    private static final String FOLDER_DESTINATION = "/model";
    private static final List<String> TYPES_SUPPORTED = Arrays.asList("String", "Date", "Int", "Long");

    private final String template;
    private final FileGenerator fileGenerator;
    private final QuarkusCommandInvocation quarkusCommandInvocation;
    private final MavenProject mavenProject;

    public ModelGenerator(String template, QuarkusCommandInvocation quarkusCommandInvocation, MavenProject mavenProject) {
        this.template = template;
        this.quarkusCommandInvocation = quarkusCommandInvocation;
        this.fileGenerator = new FileGenerator(this.quarkusCommandInvocation, mavenProject);
        this.mavenProject = mavenProject;
    }

    @Override
    public void generate(String params) {
        if (!this.validate(params)) {
            return;
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

    public boolean validate(String params) {
        String[] splitValidate = params.split(" ");
        if (splitValidate.length == 1) {
            quarkusCommandInvocation.log().error("you must have attributes: youAttr:String");
            return false;
        }

        String[] attributes = splitValidate[1].split(" ");
        for (String s : attributes) {
            String[] splitParams = s.split(":");
            if (!TYPES_SUPPORTED.contains(splitParams[1])) {
                quarkusCommandInvocation.log().error("type: {} not supported", splitParams[1]);
                return false;
            }
        }

        return true;
    }

    public Model convertToModel(String params) {
        String[] splitParams = params.split(" ");
        String className = splitParams[0];

        List<Model.Attribute> attrs = new ArrayList<>();
        for (int i = 0; i < splitParams.length; i++) {
            if (i == 0)
                continue;
            String[] values = splitParams[i].split(":");
            attrs.add(new Model.Attribute(convertType(values[1]), values[0]));
        }

        return new Model(className, attrs,
                mavenProject.getGroupId() + FOLDER_DESTINATION.replace("/", "."), className.toLowerCase());
    }

    public String convertType(String value) {
        switch (value) {
            case "Int":
                return "Integer";
            case "Date":
                return "LocalDate";
            case "String":
                return "String";
            case "Long":
                return "Long";
            default:
                break;
        }
        throw new IllegalArgumentException("Type " + value + " not supported");
    }

}
