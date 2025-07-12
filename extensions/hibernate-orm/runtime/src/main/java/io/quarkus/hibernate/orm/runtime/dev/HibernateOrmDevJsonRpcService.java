package io.quarkus.hibernate.orm.runtime.dev;

import static org.hibernate.query.sqm.internal.SqmUtil.isMutation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;
import org.hibernate.query.spi.SqmQuery;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.quarkus.devui.runtime.comms.JsonRpcMessage;
import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.comms.MessageType;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProvider;
import io.quarkus.runtime.LaunchMode;

public class HibernateOrmDevJsonRpcService {

    private static final Logger LOG = Logger.getLogger(HibernateOrmDevJsonRpcService.class);

    private boolean isDev;
    private String allowedHost;

    public HibernateOrmDevJsonRpcService() {
        this.isDev = LaunchMode.current() == LaunchMode.DEVELOPMENT && !LaunchMode.isRemoteDev();
        this.allowedHost = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.datasource.dev-ui.allowed-db-host", String.class)
                .orElse(null);
    }

    public HibernateOrmDevInfo getInfo() {
        return HibernateOrmDevController.get().getInfo();
    }

    public int getNumberOfPersistenceUnits() {
        return getInfo().getPersistenceUnits().size();
    }

    public int getNumberOfEntityTypes() {
        return getInfo().getNumberOfEntities();
    }

    public int getNumberOfNamedQueries() {
        return getInfo().getNumberOfNamedQueries();
    }

    private Optional<HibernateOrmDevInfo.PersistenceUnit> findPersistenceUnit(String persistenceUnitName) {
        return getInfo().getPersistenceUnits().stream().filter(pu -> pu.getName().equals(persistenceUnitName)).findFirst();
    }

    /**
     * Execute an arbitrary {@code hql} query in the given {@code persistence unit}. The query might be both a selection or a
     * mutation statement. For selection queries, the result count is retrieved though a count query and the results, paginated
     * based on pageNumber and pageSize are returned. For mutation statements, a custom message including the number of affected
     * records is returned.
     * <p>
     * This method handles result serialization (to JSON) internally, and returns a {@link JsonRpcMessage<String>} to avoid
     * further processing by the {@link JsonRpcRouter}.
     *
     * @param persistenceUnit The name of the persistence unit within which the query will be executed
     * @param hql The Hibernate Query Language (HQL) statement to execute
     * @param pageNumber The page number, used for selection query results pagination
     * @param pageSize The page size, used for selection query results pagination
     * @return a {@link JsonRpcMessage<String>} containing the resulting {@link DataSet} serialized to JSON.
     */
    public JsonRpcMessage<Object> executeHQL(String persistenceUnit, String hql, Integer pageNumber, Integer pageSize) {
        if (!isDev) {
            return errorDataSet("This method is only allowed in dev mode");
        }

        if (!hqlIsValid(hql)) {
            return errorDataSet("The provided HQL was not valid");
        }

        Optional<HibernateOrmDevInfo.PersistenceUnit> pu = findPersistenceUnit(persistenceUnit);
        if (pu.isEmpty()) {
            return errorDataSet("No such persistence unit: " + persistenceUnit);
        }

        //noinspection resource
        SessionFactoryImplementor sf = pu.get().sessionFactory();

        // Check the connection for this persistence unit points to an allowed datasource
        ConnectionProvider connectionProvider = sf.getServiceRegistry().requireService(ConnectionProvider.class);
        if (connectionProvider instanceof QuarkusConnectionProvider quarkusConnectionProvider) {
            if (!isAllowedDatabase(quarkusConnectionProvider.getDataSource())) {
                return errorDataSet("The persistence unit's datasource points to a non-allowed datasource. "
                        + "By default only local databases are enabled; you can use the 'quarkus.datasource.dev-ui.allowed-db-host'"
                        + " configuration property to configure allowed hosts ('*' to allow all).");
            }
        } else {
            return errorDataSet("Unsupported Connection Provider type for specified persistence unit.");
        }

        return sf.fromSession(session -> {
            Transaction transaction = session.beginTransaction();
            try {
                Query<Object> query = session.createQuery(hql, null);
                if (isMutation(((SqmQuery) query).getSqmStatement())) {
                    // DML query, execute update within transaction and return custom message with affected rows
                    int updateCount = query.executeUpdate();
                    transaction.commit();
                    return new JsonRpcMessage<>(
                            new DataSet(
                                    null,
                                    -1,
                                    "Query executed correctly. Rows affected: " + updateCount,
                                    null),
                            MessageType.Response);
                } else {
                    // selection query, execute count query and return paged results

                    // This executes a separate count query
                    long resultCount = query.getResultCount();

                    try (ScrollableResults<Object> scroll = query.scroll(ScrollMode.SCROLL_INSENSITIVE)) {
                        boolean hasNext = scroll.scroll((pageNumber - 1) * pageSize + 1);
                        List<Object> results = new ArrayList<>();
                        int i = 0;
                        while (hasNext && i++ < pageSize) {
                            results.add(scroll.get());
                            hasNext = scroll.next();
                        }

                        // manually serialize data within the transaction to ensure lazy-loading can function
                        String result = writeValueAsString(new DataSet(results, resultCount, null, null));
                        JsonRpcMessage<Object> message = new JsonRpcMessage<>(result, MessageType.Response);
                        message.setAlreadySerialized(true);
                        transaction.commit();
                        return message;
                    }
                }
            } catch (Exception ex) {
                // an error happened, rollback the transaction
                transaction.rollback();
                return new JsonRpcMessage<>(new DataSet(null, -1, null, ex.getMessage()), MessageType.Response);
            }
        });
    }

    private static JsonRpcMessage<Object> errorDataSet(String errorMessage) {
        return new JsonRpcMessage<>(new DataSet(null, -1, null, errorMessage), MessageType.Response);
    }

    private static String writeValueAsString(DataSet value) {
        try {
            JsonRpcRouter jsonRpcRouter = CDI.current().select(JsonRpcRouter.class).get();
            return jsonRpcRouter.getJsonMapper().toString(value, true);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Unable to encode results as JSON. Note circular associations are not supported at the moment, use `@JsonIgnore` to break circles.",
                    ex);
        }
    }

    private boolean hqlIsValid(String hql) {
        return hql != null && !hql.trim().isEmpty();
    }

    private boolean isAllowedDatabase(AgroalDataSource ads) {
        final String allowedHost = this.allowedHost == null ? null : this.allowedHost.trim();
        if (allowedHost != null && allowedHost.equals("*")) {
            // special value indicating to allow any host
            return true;
        }

        AgroalDataSourceConfiguration configuration = ads.getConfiguration();
        String jdbcUrl = configuration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl();

        try {
            if (jdbcUrl.startsWith("jdbc:h2:mem:") || jdbcUrl.startsWith("jdbc:h2:file:")
                    || jdbcUrl.startsWith("jdbc:h2:tcp://localhost")
                    || (allowedHost != null && !allowedHost.isBlank()
                            && jdbcUrl.startsWith("jdbc:h2:tcp://" + allowedHost))
                    || jdbcUrl.startsWith("jdbc:derby:memory:")) {
                return true;
            }

            String cleanUrl = jdbcUrl.replace("jdbc:", "");
            URI uri = new URI(cleanUrl);

            String host = uri.getHost();

            return host != null && ((host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1")) ||
                    (allowedHost != null && !allowedHost.isBlank() && host.equalsIgnoreCase(allowedHost)));

        } catch (URISyntaxException e) {
            LOG.warn(e.getMessage());
        }

        return false;
    }

    private record DataSet(List<Object> data, long totalNumberOfElements, String message, String error) {
    }
}
