package io.quarkus.hibernate.orm.dev.ui;

import static org.hibernate.query.sqm.internal.SqmUtil.isMutation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;
import org.hibernate.query.spi.SqmQuery;
import org.hibernate.tool.language.internal.MetamodelJsonSerializerImpl;
import org.hibernate.tool.language.internal.ResultsJsonSerializerImpl;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.hibernate.orm.dev.HibernateOrmDevController;
import io.quarkus.hibernate.orm.dev.HibernateOrmDevInfo;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProvider;
import io.quarkus.runtime.LaunchMode;

public class HibernateOrmDevJsonRpcService {

    private static final Logger LOG = Logger.getLogger(HibernateOrmDevJsonRpcService.class);

    private final boolean isDev;
    private final String allowedHost;

    @Inject
    Instance<Optional<Assistant>> assistant;

    public HibernateOrmDevJsonRpcService() {
        this.isDev = LaunchMode.current().isDev() && !LaunchMode.current().isRemoteDev();
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

    private static final String SYSTEM_MESSAGE = """
            You are an expert in writing Hibernate Query Language (HQL) queries.
            You have access to a entity model with the following structure:

            {{metamodel}}


            """;

    private static final String ENGLISH_TO_HQL_USER_PROMPT = """
                    If a user asks a question that can be answered by querying this model, generate an HQL SELECT query.
                    The query must not include any input parameters.
                    The field called `hql` should contain the HQL query, nothing else, no explanation, and do not put the query in backticks.
                    Example hql field value: "select e from Entity e where e.property = :value"

                    Here is the user input:
            """;

    static final record EnglishToHQLResponse(String hql) {
    }

    private static final String INTERACTIVE_USER_PROMPT = """
            The following HQL query:
            {{query}}
            returned the following data (in JSON format):
            {{data}}

            Based on the data above, answer this request in natural language:

            {{user_request}}

            The `naturalLanguageResponse` field should contain the natural language response.
            Do not include any HQL query in your response, nor suggest any further steps to take.
            """;

    static final record InteractiveResponse(String naturalLanguageResponse) {
    }

    /**
     * Execute an arbitrary {@code hql} query in the given {@code persistence unit}. The query might be both a selection or a
     * mutation statement. For selection queries, the result count is retrieved though a count query and the results, paginated
     * based on pageNumber and pageSize are returned. For mutation statements, a custom message including the number of affected
     * records is returned.
     * <p>
     * This method handles result serialization (to JSON) internally, and returns a JsonRpc Message in Map format to avoid
     * further processing by the Dev UI JsonRpcRouter.
     *
     * @param persistenceUnit The name of the persistence unit within which the query will be executed
     * @param query The user query (be it an HQL query or a plain-text statement when using assistant)
     * @param pageNumber The page number, used for selection query results pagination
     * @param pageSize The page size, used for selection query results pagination
     * @param assistant Whether to use the assistant to generate the HQL query based on the user input
     * @param interactive Enable assistant's interactive mode, answering the original user request in natural language
     * @return a JsonRpcMessage containing the resulting {@link DataSet} serialized to JSON.
     */
    public CompletionStage<Map<String, String>> executeHQL(
            String persistenceUnit,
            String query,
            Integer pageNumber,
            Integer pageSize,
            Boolean assistant,
            Boolean interactive) {
        if (!isDev) {
            return errorDataSet("This method is only allowed in dev mode");
        }

        if (!hqlIsValid(query)) {
            return errorDataSet("The provided HQL was not valid");
        }

        Optional<HibernateOrmDevInfo.PersistenceUnit> pu = findPersistenceUnit(persistenceUnit);
        if (pu.isEmpty()) {
            return errorDataSet("No such persistence unit: " + persistenceUnit);
        }

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

        if (Boolean.TRUE.equals(assistant)) {
            if (!this.assistant.isResolvable()) {
                return errorDataSet(
                        "The assistant is not available, please install the Chappie extension.");
            }
            Assistant a = this.assistant.get().orElse(null);
            if (a == null || !a.isAvailable()) {
                return errorDataSet(
                        "The assistant is not available, please check the Quarkus assistant extension is correctly configured.");
            }

            String metamodel = MetamodelJsonSerializerImpl.INSTANCE.toString(sf.getMetamodel());

            CompletionStage<EnglishToHQLResponse> queryCompletionStage = a.assistBuilder()
                    .systemMessage(SYSTEM_MESSAGE)
                    .userMessage(ENGLISH_TO_HQL_USER_PROMPT + query)
                    .addVariable("metamodel", metamodel)
                    .responseType(EnglishToHQLResponse.class)
                    .assist();

            CompletionStage<DataSet> dataSetCompletionStage = queryCompletionStage.thenApply(response -> {
                String hql = response.hql();
                if (hql == null || hql.isBlank()) {
                    return new DataSet(null, null, -1, null, "The assistant did not return a valid HQL query.");
                }
                return executeHqlQuery(hql, sf, null, null);
            });

            if (Boolean.TRUE.equals(interactive)) {
                return dataSetCompletionStage.thenCompose(dataSet -> {
                    if (dataSet.error() != null) {
                        // If there was an error executing the query, return it directly
                        return CompletableFuture.completedStage(toMap(dataSet));
                    }
                    CompletionStage<InteractiveResponse> interactiveCompletionStage = a.assistBuilder()
                            .systemMessage(SYSTEM_MESSAGE)
                            .addVariable("metamodel", metamodel)
                            .userMessage(INTERACTIVE_USER_PROMPT)
                            .addVariable("query", dataSet.query())
                            .addVariable("data", dataSet.data())
                            .addVariable("user_request", query)
                            .responseType(InteractiveResponse.class)
                            .assist();
                    return interactiveCompletionStage.thenApply(response -> {
                        String naturalLanguageResponse = response.naturalLanguageResponse();
                        return messageDataset(dataSet.query(), naturalLanguageResponse, dataSet.resultCount());
                    });
                });
            } else {
                return dataSetCompletionStage.thenApply(HibernateOrmDevJsonRpcService::toMap);
            }
        } else {
            DataSet result = executeHqlQuery(query, sf, pageNumber, pageSize);
            return CompletableFuture.completedStage(toMap(result));
        }
    }

    private static DataSet executeHqlQuery(String hql, SessionFactoryImplementor sf, Integer pageNumber, Integer pageSize) {
        return sf.fromSession(session -> {
            Transaction transaction = session.beginTransaction();
            try {
                Query<Object> query = session.createQuery(hql, null);
                if (isMutation(((SqmQuery) query).getSqmStatement())) {
                    // DML query, execute update within transaction and return custom message with affected rows
                    int updateCount = query.executeUpdate();
                    transaction.commit();
                    return new DataSet(
                            null,
                            hql,
                            -1,
                            "Query executed correctly. Rows affected: " + updateCount,
                            null);
                } else {
                    // selection query, execute count query and return paged results
                    List<Object> results;
                    long resultCount;
                    if (pageNumber != null && pageSize != null) {
                        // This executes a separate count query
                        resultCount = query.getResultCount();

                        // scroll the current page results
                        try (ScrollableResults<Object> scroll = query.scroll(ScrollMode.SCROLL_INSENSITIVE)) {
                            boolean hasNext = scroll.scroll((pageNumber - 1) * pageSize + 1);
                            results = new ArrayList<>(pageSize);
                            int i = 0;
                            while (hasNext && i++ < pageSize) {
                                results.add(scroll.get());
                                hasNext = scroll.next();
                            }
                        }
                    } else {
                        results = query.getResultList();
                        resultCount = results.size();
                    }

                    // manually serialize data within the transaction to ensure lazy-loading can function
                    ResultsJsonSerializerImpl serializer = new ResultsJsonSerializerImpl(sf);
                    String json = serializer.toString(results, query);
                    DataSet ds = new DataSet(
                            json,
                            hql,
                            resultCount,
                            null,
                            null);
                    transaction.commit();
                    return ds;
                }
            } catch (Exception ex) {
                LOG.error("Error executing HQL query", ex);
                // an error happened, rollback the transaction
                transaction.rollback();
                return new DataSet(null, null, -1, null, ex.getMessage());
            }
        });
    }

    private static CompletionStage<Map<String, String>> errorDataSet(String errorMessage) {
        return CompletableFuture.completedStage(toMap(new DataSet(null, null, -1, null, errorMessage)));
    }

    private static Map<String, String> messageDataset(String query, String message, long resultCount) {
        return toMap(new DataSet(null, query, resultCount, message, null));
    }

    private static Map<String, String> toMap(DataSet dataSet) {
        StringBuilder jsonBuilder = new StringBuilder("{");
        jsonBuilder.append("\"resultCount\":").append(dataSet.resultCount());
        if (dataSet.data() != null) {
            jsonBuilder.append(",\"data\":").append(dataSet.data());
        }
        appendIfNonNull(jsonBuilder, "query", dataSet.query());
        appendIfNonNull(jsonBuilder, "message", dataSet.message());
        appendIfNonNull(jsonBuilder, "error", dataSet.error());
        jsonBuilder.append("}");

        Map<String, String> map = Map.of(
                "response", jsonBuilder.toString(),
                "messageType", "Response",
                "alreadySerialized", "true");

        return map;
    }

    private static void appendIfNonNull(StringBuilder sb, String fieldName, String value) {
        if (value != null) {
            sb.append(",\"").append(fieldName).append("\":\"").append(value).append("\"");
        }
    }

    private boolean hqlIsValid(String hql) {
        return hql != null && !hql.trim().isEmpty();
    }

    private boolean isAllowedDatabase(AgroalDataSource ads) {
        String allowedHost = this.allowedHost == null ? null : this.allowedHost.trim();
        if (allowedHost != null && allowedHost.equals("*")) {
            // special value indicating to allow any host
            return true;
        }

        if (ads == null)
            return false;

        AgroalDataSourceConfiguration configuration = ads.getConfiguration();
        String jdbcUrl = configuration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl();

        if (jdbcUrl.startsWith("jdbc:h2:mem:") || jdbcUrl.startsWith("jdbc:h2:file:")
                || jdbcUrl.startsWith("jdbc:h2:tcp://localhost")
                || (allowedHost != null && !allowedHost.isBlank()
                        && jdbcUrl.startsWith("jdbc:h2:tcp://" + allowedHost))
                || jdbcUrl.startsWith("jdbc:derby:memory:")) {
            return true;
        }

        String cleanUrl = jdbcUrl.replace("jdbc:", "").replaceFirst(";", "?").replace(";", "&");

        try {
            URI uri = new URI(cleanUrl);
            String host = uri.getHost();
            return host != null && ((host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1")) ||
                    (allowedHost != null && !allowedHost.isBlank() && host.equalsIgnoreCase(allowedHost)));

        } catch (URISyntaxException e) {
            LOG.warn(e.getMessage());
        }

        return false;
    }

    public record DataSet(String data, String query, long resultCount, String message, String error) {
    }
}
