package io.quarkus.hibernate.reactive.runtime;

import static io.quarkus.reactive.transaction.TransactionalInterceptorBase.TRANSACTIONAL_METHOD_KEY;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.delegation.MutinySessionDelegator;
import org.hibernate.reactive.mutiny.delegation.MutinyStatelessSessionDelegator;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeDescriptor;
import io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Recorder
public class HibernateReactiveRecorder {
    private final RuntimeValue<HibernateOrmRuntimeConfig> runtimeConfig;

    public HibernateReactiveRecorder(final RuntimeValue<HibernateOrmRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public static final OpenedSessionsState<Mutiny.Session> OPENED_SESSIONS_STATE = new OpenedSessionsStateStatefulImpl();
    public static final OpenedSessionsState<Mutiny.StatelessSession> OPENED_SESSIONS_STATE_STATELESS = new OpenedSessionsStateStatelessImpl();

    /**
     * The feature needs to be initialized, even if it's not enabled.
     *
     * @param enabled Set to false if it's not being enabled, to log appropriately.
     */
    public void callHibernateReactiveFeatureInit(boolean enabled) {
        HibernateReactive.featureInit(enabled);
    }

    public void initializePersistenceProvider(
            Map<String, List<HibernateOrmIntegrationRuntimeDescriptor>> integrationRuntimeDescriptors) {
        ReactivePersistenceProviderSetup.registerRuntimePersistenceProvider(runtimeConfig.getValue(),
                integrationRuntimeDescriptors);
    }

    public Supplier<ActiveResult> checkActiveSupplier(String puName, Optional<String> dataSourceName,
            Set<String> entityClassNames) {
        return new Supplier<>() {
            @Override
            public ActiveResult get() {
                Optional<Boolean> active = runtimeConfig.getValue().persistenceUnits().get(puName).active();
                if (active.isPresent() && !active.get()) {
                    return ActiveResult.inactive(
                            PersistenceUnitUtil.persistenceUnitInactiveReasonDeactivated(puName, dataSourceName));
                }

                if (entityClassNames.isEmpty() && dataSourceName.isPresent()) {
                    // Persistence units are inactive when the corresponding datasource is inactive.
                    var dataSourceBean = ReactiveDataSourceUtil.dataSourceInstance(dataSourceName.get()).getHandle().getBean();
                    var dataSourceActive = dataSourceBean.checkActive();
                    if (!dataSourceActive.value()) {
                        return ActiveResult.inactive(
                                String.format(Locale.ROOT,
                                        "Persistence unit '%s' was deactivated automatically because it doesn't include any entity type and its datasource '%s' was deactivated.",
                                        puName,
                                        dataSourceName.get()),
                                dataSourceActive);
                    }
                }

                return ActiveResult.active();
            }
        };
    }

    public Function<SyntheticCreationalContext<Mutiny.SessionFactory>, Mutiny.SessionFactory> mutinySessionFactory(
            String persistenceUnitName) {
        return new Function<SyntheticCreationalContext<Mutiny.SessionFactory>, Mutiny.SessionFactory>() {
            @Override
            public Mutiny.SessionFactory apply(SyntheticCreationalContext<Mutiny.SessionFactory> context) {
                JPAConfig jpaConfig = context.getInjectedReference(JPAConfig.class);

                SessionFactory sessionFactory = jpaConfig
                        .getEntityManagerFactory(persistenceUnitName, true)
                        .unwrap(SessionFactory.class);

                return sessionFactory.unwrap(Mutiny.SessionFactory.class);
            }
        };
    }

    public Function<SyntheticCreationalContext<Mutiny.Session>, Mutiny.Session> sessionSupplier(String persistenceUnitName) {
        return new Function<SyntheticCreationalContext<Mutiny.Session>, Mutiny.Session>() {

            @Override
            public Mutiny.Session apply(SyntheticCreationalContext<Mutiny.Session> context) {
                return new MutinySessionDelegator() {
                    @Override
                    public Mutiny.Session delegate() {
                        return getSession(persistenceUnitName);
                    }
                };
            }
        };
    }

    public static Mutiny.Session getSession(String persistenceUnitName) {
        Context context = Vertx.currentContext();

        Optional<OpenedSessionsState.SessionWithKey<Mutiny.Session>> openedSession = OPENED_SESSIONS_STATE.getOpenedSession(
                context,
                persistenceUnitName);
        // reuse the existing reactive session
        if (openedSession.isPresent()) {
            return openedSession.get().session();
        } else if (context.getLocal(TRANSACTIONAL_METHOD_KEY) == null) {
            throw new IllegalStateException("No current Mutiny.Session found"
                    + "\n\t- no reactive session was found in the Vert.x context and the context was not marked to open a new session lazily"
                    + "\n\t- a session is opened automatically for JAX-RS resource methods annotated with an HTTP method (@GET, @POST, etc.); inherited annotations are not taken into account"
                    + "\n\t- you may need to annotate the business method with @Transactional");
        } else {

            Optional<OpenedSessionsState.SessionWithKey<Mutiny.StatelessSession>> openedStatelessSession = OPENED_SESSIONS_STATE_STATELESS
                    .getOpenedSession(
                            context,
                            persistenceUnitName);

            if (openedStatelessSession.isPresent()) {
                throw new IllegalStateException("A stateless session for the same Persistence Unit is already opened."
                        + "\n\t- Mixing different kinds of sessions is forbidden");
            }

            return OPENED_SESSIONS_STATE.createNewSession(persistenceUnitName, context);
        }
    }

    public Function<SyntheticCreationalContext<Mutiny.StatelessSession>, Mutiny.StatelessSession> statelessSessionSupplier(
            String persistenceUnitName) {
        return new Function<SyntheticCreationalContext<Mutiny.StatelessSession>, Mutiny.StatelessSession>() {

            @Override
            public Mutiny.StatelessSession apply(SyntheticCreationalContext<Mutiny.StatelessSession> context) {
                return new MutinyStatelessSessionDelegator() {
                    @Override
                    public Mutiny.StatelessSession delegate() {
                        return getStatelessSession(persistenceUnitName);
                    }
                };
            }
        };
    }

    public static Mutiny.StatelessSession getStatelessSession(String persistenceUnitName) {
        Context context = Vertx.currentContext();

        Optional<OpenedSessionsState.SessionWithKey<Mutiny.StatelessSession>> openedSession = OPENED_SESSIONS_STATE_STATELESS
                .getOpenedSession(context, persistenceUnitName);
        // reuse the existing reactive session
        if (openedSession.isPresent()) {
            return openedSession.get().session();
        } else if (context.getLocal(TRANSACTIONAL_METHOD_KEY) == null) {
            throw new IllegalStateException("No current Mutiny.Session found"
                    + "\n\t- no reactive session was found in the Vert.x context and the context was not marked to open a new session lazily"
                    + "\n\t- a session is opened automatically for JAX-RS resource methods annotated with an HTTP method (@GET, @POST, etc.); inherited annotations are not taken into account"
                    + "\n\t- you may need to annotate the business method with @Transactional");
        } else {

            Optional<OpenedSessionsState.SessionWithKey<Mutiny.Session>> openedRegularSession = OPENED_SESSIONS_STATE
                    .getOpenedSession(
                            context,
                            persistenceUnitName);

            if (openedRegularSession.isPresent()) {
                throw new IllegalStateException("A session for the same Persistence Unit is already opened."
                        + "\n\t- Mixing different kinds of sessions is forbidden");
            }

            return OPENED_SESSIONS_STATE_STATELESS.createNewSession(persistenceUnitName, context);
        }
    }

}
