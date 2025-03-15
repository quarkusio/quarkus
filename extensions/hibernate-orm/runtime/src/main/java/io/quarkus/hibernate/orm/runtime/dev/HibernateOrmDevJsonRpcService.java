package io.quarkus.hibernate.orm.runtime.dev;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.SelectionQuery;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProvider;
import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;

public class HibernateOrmDevJsonRpcService {
    private boolean isDev = false;
    private String allowedHost = null;

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

    public DataSet executeHQL(String persistenceUnit, String hql, Integer pageNumber, Integer pageSize) {
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
                return errorDataSet("The persistence unit datasource points to a non-allowed datasource " +
                        "(by default, only local databases are allowed).");
            }
        } else {
            return errorDataSet("Unsupported Connection Provider type for specified persistence unit.");
        }

        return sf.fromSession(session -> {
            try {
                // Hibernate ensures the provided HQL is a selection statement, no pre-validation needed
                SelectionQuery<Object> query = session.createSelectionQuery(hql, Object.class);

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

                    // todo : for now we rely on automatic marshalling of results
                    return new DataSet(results, resultCount, null);
                }
            } catch (Exception ex) {
                return errorDataSet(ex.getMessage());
            }
        });
    }

    private static DataSet errorDataSet(String errorMessage) {
        return new DataSet(null, -1, errorMessage);
    }

    private boolean hqlIsValid(String hql) {
        return hql != null && !hql.trim().isEmpty();
    }

    private boolean isAllowedDatabase(AgroalDataSource ads) {
        AgroalDataSourceConfiguration configuration = ads.getConfiguration();
        String jdbcUrl = configuration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl();

        try {
            if (jdbcUrl.startsWith("jdbc:h2:mem:") || jdbcUrl.startsWith("jdbc:h2:file:")
                    || jdbcUrl.startsWith("jdbc:h2:tcp://localhost")
                    || (this.allowedHost != null && !this.allowedHost.isBlank()
                            && jdbcUrl.startsWith("jdbc:h2:tcp://" + this.allowedHost))
                    || jdbcUrl.startsWith("jdbc:derby:memory:")) {
                return true;
            }

            String cleanUrl = jdbcUrl.replace("jdbc:", "");
            URI uri = new URI(cleanUrl);

            String host = uri.getHost();

            return host != null && ((host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1")) ||
                    (this.allowedHost != null && !this.allowedHost.isBlank() && host.equalsIgnoreCase(this.allowedHost)));

        } catch (URISyntaxException e) {
            Log.warn(e.getMessage());
        }

        return false;
    }

    private record DataSet(List<Object> data, long totalNumberOfElements, String error) {
    }
}
