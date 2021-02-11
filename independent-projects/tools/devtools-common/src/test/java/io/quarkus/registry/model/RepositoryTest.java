package io.quarkus.registry.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.registry.catalog.model.Repository;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class RepositoryTest {

    @Test
    void shouldParseRepository() throws Exception {
        Repository repository = Repository.parse(Paths.get("src/test/resources/registry/repository"), new ObjectMapper());
        assertThat(repository).isNotNull();
        assertThat(repository.getPlatforms()).isNotEmpty();
        assertThat(repository.getIndividualExtensions()).isNotEmpty();
    }
}
