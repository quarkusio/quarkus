package io.quarkus.platform.templates;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

public class ProjectPomTemplateFactory {

    public static ProjectConfigTemplate forPom(Model model) {
        final ProjectConfigTemplate.Builder builder = ProjectConfigTemplate.builder();

        addProperties(model, builder);

        final List<Dependency> depMgmt = model.getDependencyManagement().getDependencies();
        if(!depMgmt.isEmpty()) {
            for(Dependency dep : depMgmt) {
                if(dep.getScope().equals("import") && dep.getType().equals("pom")) {
                    builder.setPlatformBom(
                            new BomTemplate(
                                    new ElementTemplate("groupId", propNameOrNull(dep.getGroupId())),
                                    new ElementTemplate("artifactId", propNameOrNull(dep.getArtifactId())),
                                    new ElementTemplate("version", propNameOrNull(dep.getVersion()))));
                }
            }
        }

        return builder.build();
    }

    private static void addProperties(Model model, final ProjectConfigTemplate.Builder builder) {
        final Properties pomProps = model.getProperties();
        if (!pomProps.isEmpty()) {
            for (Map.Entry<Object, Object> prop : pomProps.entrySet()) {
                builder.addProperty(
                        new PropertyTemplate(prop.getKey().toString(),
                                propNameOrNull(prop.getValue().toString())));
            }
        }
    }

    private static String propNameOrNull(String value) {
        return isPropertyExpr(value) ? extractPropNameFromExpr(value) : null;
    }

    private static String extractPropNameFromExpr(String value) {
        return value.substring(2, value.length() - 1);
    }

    private static boolean isPropertyExpr(String value) {
        return value.startsWith("${") && value.endsWith("}");
    }
}
