package io.quarkus.platform.descriptor;

import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import java.io.IOException;
import java.util.List;
import org.apache.maven.model.Dependency;

public interface QuarkusPlatformDescriptor {

    String getBomGroupId();

    String getBomArtifactId();

    String getBomVersion();

    String getQuarkusVersion();

    List<Dependency> getManagedDependencies();

    List<Extension> getExtensions();

    List<Category> getCategories();

    String getTemplate(String name);

    <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException;
}
