package io.quarkus.devtools.commands.handlers;

import static org.mockito.Mockito.mock;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScaffoldCommandHandlerTest {

    public ScaffoldCommandHandler scaffoldCommandHandler;
    public ExtensionCatalog extensionCatalog;
    public MessageWriter messageWriter;
    public ExtensionManager extensionManager;

    @BeforeEach
    public void before() {
        scaffoldCommandHandler = new ScaffoldCommandHandler();
        extensionCatalog = mock(ExtensionCatalog.class);
        messageWriter = mock(MessageWriter.class);
        extensionManager = mock(ExtensionManager.class);
    }

    @Test
    public void shouldGeneratePanacheModel() throws QuarkusCommandException {
        QuarkusProject quarkusProject = QuarkusProject.of(
                Paths.get("/home/jose-da-silva-neto/Desktop/github/forks/quarkus/independent-projects/tools/devtools-common"),
                extensionCatalog, Collections.emptyList(),
                messageWriter, extensionManager);

        QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(quarkusProject);
        scaffoldCommandHandler.execute(invocation);
    }

}
