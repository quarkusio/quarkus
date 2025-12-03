package io.quarkus.oidc.db.token.state.manager.runtime;

import java.util.function.Supplier;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.oidc.db.token.state.manager.runtime.OidcDbTokenStateManagerInitializer.SupportedReactiveSqlClient;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.sqlclient.Pool;

@Recorder
public class OidcDbTokenStateManagerRecorder {

    /* STATIC INIT */
    public Supplier<OidcDbTokenStateManager> createTokenStateManager(String insertStatement, String deleteStatement,
            String getQuery) {
        return new Supplier<OidcDbTokenStateManager>() {
            @Override
            public OidcDbTokenStateManager get() {
                return new OidcDbTokenStateManager(insertStatement, deleteStatement, getQuery);
            }
        };
    }

    /* RUNTIME INIT */
    public void setSqlClientPool(BeanContainer container) {
        container.beanInstance(OidcDbTokenStateManager.class).setSqlClientPool(container.beanInstance(Pool.class));
    }

    /* STATIC INIT */
    public BeanContainerListener setSupportedReactiveSqlClient(SupportedReactiveSqlClient supportedReactiveSqlClient) {
        return container -> container.beanInstance(OidcDbTokenStateManagerInitializer.class)
                .setSupportedReactiveSqlClient(supportedReactiveSqlClient);
    }
}
