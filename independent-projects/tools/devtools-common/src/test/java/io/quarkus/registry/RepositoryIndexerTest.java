package io.quarkus.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.catalog.model.Repository;
import io.quarkus.registry.catalog.spi.IndexVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class RepositoryIndexerTest {
    @Test
    void shouldVisitParsedElements() throws Exception {
        Path rootPath = Paths.get("src/test/resources/registry/repository");
        assertThat(rootPath).exists();
        ObjectMapper mapper = new ObjectMapper();
        Repository repository = Repository.parse(rootPath, mapper);
        RepositoryIndexer indexer = new RepositoryIndexer(new TestArtifactResolver());
        IndexVisitor mock = mock(IndexVisitor.class);
        indexer.index(repository, mock);
        verify(mock, atLeast(4)).visitPlatform(any(QuarkusPlatformDescriptor.class));
        verify(mock, atLeast(2)).visitExtension(any(Extension.class), anyString());
    }
}
