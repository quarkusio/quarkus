package io.quarkus.hibernate.orm.runtime.observers;

import java.util.concurrent.CompletableFuture;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.DelayedDropAction;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import io.quarkus.hibernate.orm.runtime.SchemaToolingUtil;

public final class SessionFactoryObserverForSchemaExport implements SessionFactoryObserver {
    private final MetadataImplementor metadata;
    private DelayedDropAction delayedDropAction;

    public SessionFactoryObserverForSchemaExport(MetadataImplementor metadata) {
        this.metadata = metadata;
    }

    @Override
    public void sessionFactoryCreated(SessionFactory factory) {
        SchemaManagementToolCoordinator.process(
                metadata,
                getRegistry(factory),
                factory.getProperties(),
                action -> delayedDropAction = action);
        CompletableFuture.runAsync(SchemaToolingUtil::deleteSqlImportTempDirectories);
    }

    @Override
    public void sessionFactoryClosing(SessionFactory factory) {
        if (delayedDropAction != null) {
            delayedDropAction.perform(getRegistry(factory));
        }
    }

    private static ServiceRegistryImplementor getRegistry(SessionFactory factory) {
        return ((SessionFactoryImplementor) factory).getServiceRegistry();
    }
}
