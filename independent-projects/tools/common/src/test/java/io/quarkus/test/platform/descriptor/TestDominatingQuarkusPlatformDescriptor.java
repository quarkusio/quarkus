package io.quarkus.test.platform.descriptor;

import static io.quarkus.test.platform.descriptor.loader.QuarkusTestPlatformDescriptorLoader.addCategory;
import static io.quarkus.test.platform.descriptor.loader.QuarkusTestPlatformDescriptorLoader.addExtension;

import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;

public class TestDominatingQuarkusPlatformDescriptor implements QuarkusPlatformDescriptor {

    private final List<Category> categories = new ArrayList<>();
    private final List<Extension> extensions = new ArrayList<>();
    private final List<Dependency> bomDeps = new ArrayList<>();

    public TestDominatingQuarkusPlatformDescriptor() {

        addCategory("other", "Other category", categories);
        addCategory("web", "Dominating Web", categories);

        addExtension("io.quarkus", "quarkus-resteasy", "dominating-version", "Dominating RESTEasy", "dominating/guide",
                extensions, bomDeps);
    }

    @Override
    public String getBomGroupId() {
        return "dominating.bom.group.id";
    }

    @Override
    public String getBomArtifactId() {
        return "dominating.bom.artifact.id";
    }

    @Override
    public String getBomVersion() {
        return "dominating.bom.version";
    }

    @Override
    public String getQuarkusVersion() {
        return "dominating.quarkus.version";
    }

    @Override
    public List<Dependency> getManagedDependencies() {
        return bomDeps;
    }

    @Override
    public List<Extension> getExtensions() {
        return extensions;
    }

    @Override
    public List<Category> getCategories() {
        return categories;
    }

    @Override
    public String getTemplate(String name) {
        if ("templates/basic-rest/java/pom.xml-template.ftl".equals(name)) {
            return "dominating pom.xml template";
        }
        return null;
    }

    @Override
    public <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
        return null;
    }
}
