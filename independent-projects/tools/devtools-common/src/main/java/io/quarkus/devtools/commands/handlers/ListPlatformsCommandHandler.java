package io.quarkus.devtools.commands.handlers;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;

public class ListPlatformsCommandHandler implements QuarkusCommandHandler {

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final ExtensionCatalog catalog = invocation.getQuarkusProject().getExtensionsCatalog();
        logPlatform(catalog, invocation.log());
        catalog.getDerivedFrom().forEach(o -> logPlatform(o, invocation.log()));
        return QuarkusCommandOutcome.success();
    }

    private static void logPlatform(ExtensionOrigin o, MessageWriter log) {
        final ArtifactCoords bom = o.isPlatform() ? o.getBom() : null;
        if (bom != null) {
            log.info(bom.toString());
        }
    }
}
