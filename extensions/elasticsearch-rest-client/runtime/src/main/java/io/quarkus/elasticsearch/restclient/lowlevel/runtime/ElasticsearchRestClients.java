package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;

import static java.util.stream.Collectors.toUnmodifiableSet;

@ApplicationScoped
public class ElasticsearchRestClients {

    private static final Logger LOG = Logger.getLogger(ElasticsearchRestClients.class);

    private final ElasticsearchClientsRuntimeConfig runtimeConfig;
    private final Set<String> inactiveNames;

    private final ConcurrentMap<String, RestClientHolder> clients = new ConcurrentHashMap<>();

    public ElasticsearchRestClients(ElasticsearchClientsRuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
        Stream.Builder<String> inactive = Stream.builder();
        for (Map.Entry<String, ElasticsearchClientRuntimeConfig> entry : runtimeConfig.clients().entrySet()) {
            // TODO add an "active" config -- preferably in a different commit
            if (!entry.getValue().active()) {
                inactive.add(entry.getKey());
            }
        }
        this.inactiveNames = inactive.build().collect(toUnmodifiableSet());
    }

    public boolean isRestClientCreated(String dataSourceName) {
        return clients.containsKey(dataSourceName);
    }

    public Set<String> getActiveDataSourceNames() {
        // Datasources are created on startup,
        // and we only create active datasources.
        return clients.keySet();
    }

    public AgroalDataSource getDataSource(String dataSourceName) {
        return clients.computeIfAbsent(dataSourceName, new Function<String, AgroalDataSource>() {
            @Override
            public AgroalDataSource apply(String s) {
                return doCreateDataSource(s, true);
            }
        });
    }

    @PostConstruct
    public void start() {
        for (String dataSourceName : agroalDataSourceSupport.entries.keySet()) {
            clients.computeIfAbsent(dataSourceName, new Function<String, AgroalDataSource>() {
                @Override
                public AgroalDataSource apply(String s) {
                    return doCreateDataSource(s, false);
                }
            });
        }
    }

    @SuppressWarnings("resource")
    public AgroalDataSource doCreateClient(String clientName, boolean failIfInactive) {
        if (!agroalDataSourceSupport.entries.containsKey(dataSourceName)) {
            throw new IllegalArgumentException("No datasource named '" + dataSourceName + "' exists");
        }

        DataSourceJdbcBuildTimeConfig dataSourceJdbcBuildTimeConfig = dataSourcesJdbcBuildTimeConfig
                .dataSources().get(dataSourceName).jdbc();
        DataSourceRuntimeConfig dataSourceRuntimeConfig = runtimeConfig.dataSources().get(dataSourceName);

        if (dataSourceSupport.getInactiveNames().contains(dataSourceName)) {
            if (failIfInactive) {
                throw DataSourceUtil.dataSourceInactive(dataSourceName);
            } else {
                // This only happens on startup, and effectively cancels the creation
                // so that we only throw an exception on first actual use.
                return null;
            }
        }

        DataSourceJdbcRuntimeConfig dataSourceJdbcRuntimeConfig = dataSourcesJdbcRuntimeConfig
                .dataSources().get(dataSourceName).jdbc();
        if (!dataSourceJdbcRuntimeConfig.url().isPresent()) {
            //this is not an error situation, because we want to allow the situation where a JDBC extension
            //is installed but has not been configured
            return new UnconfiguredDataSource(
                    DataSourceUtil.dataSourcePropertyKey(dataSourceName, "jdbc.url") + " has not been defined");
        }

        // we first make sure that all available JDBC drivers are loaded in the current TCCL
        loadDriversInTCCL();

        AgroalDataSourceSupport.Entry matchingSupportEntry = agroalDataSourceSupport.entries.get(dataSourceName);
        String resolvedDriverClass = matchingSupportEntry.resolvedDriverClass;
        Class<?> driver;
        try {
            driver = Class.forName(resolvedDriverClass, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Unable to load the datasource driver " + resolvedDriverClass + " for datasource " + dataSourceName, e);
        }

        String jdbcUrl = dataSourceJdbcRuntimeConfig.url().get();

        if (dataSourceJdbcBuildTimeConfig.tracing()) {
            boolean tracingEnabled = dataSourceJdbcRuntimeConfig.tracing().enabled()
                    .orElse(dataSourceJdbcBuildTimeConfig.tracing());

            if (tracingEnabled) {
                String rootTracingUrl = !jdbcUrl.startsWith(JDBC_TRACING_URL_PREFIX)
                        ? jdbcUrl.replace(JDBC_URL_PREFIX, JDBC_TRACING_URL_PREFIX)
                        : jdbcUrl;

                StringBuilder tracingURL = new StringBuilder(rootTracingUrl);

                if (dataSourceJdbcRuntimeConfig.tracing().traceWithActiveSpanOnly()) {
                    if (!tracingURL.toString().contains("?")) {
                        tracingURL.append("?");
                    }

                    tracingURL.append("traceWithActiveSpanOnly=true");
                }

                if (dataSourceJdbcRuntimeConfig.tracing().ignoreForTracing().isPresent()) {
                    if (!tracingURL.toString().contains("?")) {
                        tracingURL.append("?");
                    }

                    Arrays.stream(dataSourceJdbcRuntimeConfig.tracing().ignoreForTracing().get().split(";"))
                            .filter(query -> !query.isEmpty())
                            .forEach(query -> tracingURL.append("ignoreForTracing=")
                                    .append(query.replaceAll("\"", "\\\""))
                                    .append(";"));
                }

                // Override datasource URL with tracing driver prefixed URL
                jdbcUrl = tracingURL.toString();

                //remove driver class so that agroal connectionFactory will use the tracking driver anyway
                driver = null;
            }
        }

        String resolvedDbKind = matchingSupportEntry.resolvedDbKind;
        AgroalConnectionConfigurer agroalConnectionConfigurer = Arc.container()
                .instance(AgroalConnectionConfigurer.class, new JdbcDriverLiteral(resolvedDbKind))
                .orElse(new UnknownDbAgroalConnectionConfigurer());

        AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();

        // Set pool-less mode
        if (!dataSourceJdbcRuntimeConfig.poolingEnabled()) {
            dataSourceConfiguration.dataSourceImplementation(DataSourceImplementation.AGROAL_POOLLESS);
        }

        AgroalConnectionPoolConfigurationSupplier poolConfiguration = dataSourceConfiguration.connectionPoolConfiguration();
        AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration = poolConfiguration
                .connectionFactoryConfiguration();

        boolean mpMetricsPresent = agroalDataSourceSupport.mpMetricsPresent;
        applyNewConfiguration(dataSourceName, dataSourceConfiguration, poolConfiguration, connectionFactoryConfiguration,
                driver, jdbcUrl,
                dataSourceJdbcBuildTimeConfig, dataSourceRuntimeConfig, dataSourceJdbcRuntimeConfig, transactionRuntimeConfig,
                mpMetricsPresent);

        if (agroalDataSourceSupport.disableSslSupport) {
            agroalConnectionConfigurer.disableSslSupport(resolvedDbKind, dataSourceConfiguration);
        }
        //we use a custom cache for two reasons:
        //fast thread local cache should be faster
        //and it prevents a thread local leak
        try {
            Class.forName("io.netty.util.concurrent.FastThreadLocal", true, Thread.currentThread().getContextClassLoader());
            dataSourceConfiguration.connectionPoolConfiguration().connectionCache(new QuarkusNettyConnectionCache());
        } catch (ClassNotFoundException e) {
            dataSourceConfiguration.connectionPoolConfiguration().connectionCache(new QuarkusSimpleConnectionCache());
        }

        agroalConnectionConfigurer.setExceptionSorter(resolvedDbKind, dataSourceConfiguration);

        // Explicit reference to bypass reflection need of the ServiceLoader used by AgroalDataSource#from
        AgroalDataSourceConfiguration agroalConfiguration = dataSourceConfiguration.get();
        AgroalDataSource dataSource = new io.agroal.pool.DataSource(agroalConfiguration,
                new AgroalEventLoggingListener(dataSourceName,
                        agroalConfiguration.connectionPoolConfiguration()
                                .transactionRequirement() == TransactionRequirement.WARN));
        log.debugv("Started datasource {0} connected to {1}", dataSourceName,
                agroalConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl());

        // Set pool interceptors for this datasource
        Collection<AgroalPoolInterceptor> interceptorList = agroalPoolInterceptors
                .select(dataSourceName == null || DataSourceUtil.isDefault(dataSourceName)
                        ? Default.Literal.INSTANCE
                        : new DataSource.DataSourceLiteral(dataSourceName))
                .stream().collect(Collectors.toList());
        if (!interceptorList.isEmpty()) {
            dataSource.setPoolInterceptors(interceptorList);
        }

        if (dataSourceJdbcBuildTimeConfig.telemetry() && dataSourceJdbcRuntimeConfig.telemetry().orElse(true)) {
            // activate OpenTelemetry JDBC instrumentation by wrapping AgroalDatasource
            // use an optional CDI bean as we can't reference optional OpenTelemetry classes here
            dataSource = agroalOpenTelemetryWrapper.get().apply(dataSource);
        }

        return dataSource;
    }

    @PreDestroy
    public void stop() {
        for (AgroalDataSource dataSource : clients.values()) {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }
}
