package io.quarkus.platform.descriptor;

import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Dependency;

public interface QuarkusPlatformDescriptor {

    String getBomGroupId();

    String getBomArtifactId();

    String getBomVersion();

    String getQuarkusVersion();

    /**
     *
     * @return platform's dependencyManagement
     */
    default List<Dependency> getManagedDependencies() {
        throw new UnsupportedOperationException();
    }

    List<Extension> getExtensions();

    List<Category> getCategories();

    default Map<String, Object> getMetadata() {
        return Collections.emptyMap();
    }

    String getTemplate(String name);

    <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException;

    <T> T loadResourceAsPath(String name, ResourcePathConsumer<T> consumer) throws IOException;

    default String gav() {
        return String.format("%s:%s:%s", getBomGroupId(), getBomArtifactId(), getBomVersion());
    }

}
