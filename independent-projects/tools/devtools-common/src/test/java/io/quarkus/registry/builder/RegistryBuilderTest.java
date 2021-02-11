package io.quarkus.registry.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.quarkus.registry.RepositoryIndexer;
import io.quarkus.registry.TestArtifactResolver;
import io.quarkus.registry.catalog.model.Repository;
import io.quarkus.registry.model.Registry;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegistryBuilderTest {

    static Registry registry;

    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Repository repository = Repository.parse(Paths.get("src/test/resources/registry/repository"), mapper);
        RepositoryIndexer indexer = new RepositoryIndexer(new TestArtifactResolver());
        RegistryBuilder builder = new RegistryBuilder();
        indexer.index(repository, builder);
        registry = builder.build();
    }

    @Test
    void build() throws Exception {
        assertThat(registry.getExtensions()).isNotEmpty();
        assertThat(registry.getPlatforms()).isNotEmpty();
        if (Boolean.getBoolean("generateTmpRegistry")) {
            ObjectMapper mapper = new ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
            mapper.writeValue(new java.io.File("/tmp/registry.json"), registry);
        }
    }

    @Test
    void diffMap() {
        Map<String, Object> left = new HashMap<>();
        left.put("A", "1");
        left.put("B", "2");
        Map<String, Object> right = new HashMap<>();
        right.put("A", "1");
        right.put("B", "2");
        right.put("C", "3");
        Map<String, Object> result = RegistryBuilder.diff(left, right);
        assertThat(result).doesNotContainKeys("A", "B").containsOnly(entry("C", "3"));
    }

}
