package io.quarkus.registry.client.maven;

import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.registry.config.RegistryConfig;

class RegistryCacheRefreshLogger implements TransferListener {

    private final RegistryConfig config;
    private final MessageWriter log;
    private final TransferListener delegate;
    boolean loggedCatalogRefreshMsg;

    public RegistryCacheRefreshLogger(RegistryConfig config, MessageWriter log, TransferListener delegate) {
        this.config = config;
        this.log = log;
        this.delegate = delegate;
    }

    @Override
    public void transferInitiated(TransferEvent event) throws TransferCancelledException {
        if (!loggedCatalogRefreshMsg && !event.getResource().getResourceName()
                .contains(config.getDescriptor().getArtifact().getArtifactId())) {
            loggedCatalogRefreshMsg = true;
            log.info("Looking for the newly published extensions in " + config.getId());
        }
        if (delegate != null) {
            delegate.transferInitiated(event);
        }
    }

    @Override
    public void transferStarted(TransferEvent event) throws TransferCancelledException {
        if (delegate != null) {
            delegate.transferStarted(event);
        }
    }

    @Override
    public void transferProgressed(TransferEvent event) throws TransferCancelledException {
        if (delegate != null) {
            delegate.transferProgressed(event);
        }
    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        if (delegate != null) {
            delegate.transferCorrupted(event);
        }
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        if (delegate != null) {
            delegate.transferSucceeded(event);
        }
    }

    @Override
    public void transferFailed(TransferEvent event) {
        if (delegate != null) {
            delegate.transferFailed(event);
        }
    }
}
