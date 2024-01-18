package io.quarkus.hibernate.search.orm.elasticsearch.runtime.management;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.hibernate.CacheMode;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.scope.SearchScope;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

class HibernateSearchPostRequestProcessor {

    private static final String QUERY_PARAM_WAIT_FOR = "wait_for";
    private static final String QUERY_PARAM_PERSISTENCE_UNIT = "persistence_unit";

    public void process(RoutingContext ctx) {
        JsonObject config = ctx.body().asJsonObject();
        if (config == null) {
            config = new JsonObject();
        }
        try (InstanceHandle<SearchMapping> searchMappingInstanceHandle = searchMappingInstanceHandle(ctx.request())) {

            SearchMapping searchMapping = searchMappingInstanceHandle.get();

            JsonObject filter = config.getJsonObject("filter");
            List<String> types = getTypesToFilter(filter);
            Set<String> tenants = getTenants(filter);
            MassIndexer massIndexer;
            if (types == null || types.isEmpty()) {
                massIndexer = createMassIndexer(searchMapping.scope(Object.class), tenants);
            } else {
                massIndexer = createMassIndexer(searchMapping.scope(Object.class, types), tenants);
            }

            HibernateSearchMassIndexerConfiguration.configure(massIndexer, config.getJsonObject("massIndexer"));

            CompletionStage<?> massIndexerFuture = massIndexer.start();

            if (WaitFor.STARTED.equals(getWaitForParameter(ctx.request()))) {
                ctx.response().end(message(202, "Reindexing started"));
            } else {
                ctx.response()
                        .setChunked(true)
                        .write(message(202, "Reindexing started"),
                                ignored -> massIndexerFuture.whenComplete((ignored2, throwable) -> {
                                    if (throwable == null) {
                                        ctx.response().end(message(200, "Reindexing succeeded"));
                                    } else {
                                        ctx.response().end(message(
                                                500,
                                                "Reindexing failed:\n" + Arrays.stream(throwable.getStackTrace())
                                                        .map(Object::toString)
                                                        .collect(Collectors.joining("\n"))));
                                    }
                                }));
            }
        }
    }

    private MassIndexer createMassIndexer(SearchScope<Object> scope, Set<String> tenants) {
        if (tenants == null || tenants.isEmpty()) {
            return scope.massIndexer();
        } else {
            return scope.massIndexer(tenants);
        }
    }

    private List<String> getTypesToFilter(JsonObject filter) {
        if (filter == null) {
            return null;
        }
        JsonArray array = filter.getJsonArray("types");
        if (array == null) {
            return null;
        }
        List<String> types = array
                .stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        return types.isEmpty() ? null : types;
    }

    private Set<String> getTenants(JsonObject filter) {
        if (filter == null) {
            return null;
        }
        JsonArray array = filter.getJsonArray("tenants");
        if (array == null) {
            return null;
        }
        Set<String> types = array
                .stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
        return types.isEmpty() ? null : types;
    }

    private WaitFor getWaitForParameter(HttpServerRequest request) {
        return WaitFor.valueOf(request.getParam(QUERY_PARAM_WAIT_FOR, WaitFor.STARTED.name()).toUpperCase(Locale.ROOT));
    }

    private InstanceHandle<SearchMapping> searchMappingInstanceHandle(HttpServerRequest request) {
        String pu = request.getParam(QUERY_PARAM_PERSISTENCE_UNIT, PersistenceUnit.DEFAULT);
        return Arc.container().instance(SearchMapping.class, new PersistenceUnit.PersistenceUnitLiteral(pu));
    }

    private static String message(int code, String message) {
        return JsonObject.of("code", code, "message", message) + "\n";
    }

    private enum WaitFor {
        STARTED,
        FINISHED;
    }

    private static final class HibernateSearchMassIndexerConfiguration {
        private HibernateSearchMassIndexerConfiguration() {
        }

        /**
         * Sets the number of entity types to be indexed in parallel
         */
        private static final String TYPES_TO_INDEX_IN_PARALLEL = "typesToIndexInParallel";

        /**
         * Sets the number of threads to be used to load the root entities.
         */
        private static final String THREADS_TO_LOAD_OBJECTS = "threadsToLoadObjects";

        /**
         * Sets the batch size used to load the root entities.
         */
        private static final String BATCH_SIZE_TO_LOAD_OBJECTS = "batchSizeToLoadObjects";

        /**
         * Sets the cache interaction mode for the data loading tasks.
         */
        private static final String CACHE_MODE = "cacheMode";

        /**
         * If each index is merged into a single segment after indexing.
         */
        private static final String MERGE_SEGMENTS_ON_FINISH = "mergeSegmentsOnFinish";

        /**
         * If each index is merged into a single segment after the initial index purge, just before indexing.
         */
        private static final String MERGE_SEGMENTS_AFTER_PURGE = "mergeSegmentsAfterPurge";

        /**
         * If the indexes and their schema (if they exist) should be dropped and re-created before indexing.
         */
        private static final String DROP_AND_CREATE_SCHEMA_ON_START = "dropAndCreateSchemaOnStart";

        /**
         * If all entities are removed from the indexes before indexing.
         */
        private static final String PURGE_ALL_ON_START = "purgeAllOnStart";

        /**
         * Specifies the fetch size to be used when loading primary keys if objects to be indexed.
         */
        private static final String ID_FETCH_SIZE = "idFetchSize";

        /**
         * Specifies the timeout of transactions for loading ids and entities to be re-indexed.
         */
        private static final String TRANSACTION_TIMEOUT = "transactionTimeout";

        private static MassIndexer configure(MassIndexer massIndexer, JsonObject config) {
            if (config == null) {
                return massIndexer;
            }
            if (config.getInteger(TYPES_TO_INDEX_IN_PARALLEL) != null) {
                massIndexer.typesToIndexInParallel(config.getInteger(TYPES_TO_INDEX_IN_PARALLEL));
            }
            if (config.getInteger(THREADS_TO_LOAD_OBJECTS) != null) {
                massIndexer.threadsToLoadObjects(config.getInteger(THREADS_TO_LOAD_OBJECTS));
            }
            if (config.getInteger(BATCH_SIZE_TO_LOAD_OBJECTS) != null) {
                massIndexer.batchSizeToLoadObjects(config.getInteger(BATCH_SIZE_TO_LOAD_OBJECTS));
            }
            if (config.getString(CACHE_MODE) != null) {
                massIndexer.cacheMode(CacheMode.valueOf(config.getString(CACHE_MODE)));
            }
            if (config.getBoolean(MERGE_SEGMENTS_ON_FINISH) != null) {
                massIndexer.mergeSegmentsOnFinish(config.getBoolean(MERGE_SEGMENTS_ON_FINISH));
            }
            if (config.getBoolean(MERGE_SEGMENTS_AFTER_PURGE) != null) {
                massIndexer.mergeSegmentsAfterPurge(config.getBoolean(MERGE_SEGMENTS_AFTER_PURGE));
            }
            if (config.getBoolean(DROP_AND_CREATE_SCHEMA_ON_START) != null) {
                massIndexer.dropAndCreateSchemaOnStart(config.getBoolean(DROP_AND_CREATE_SCHEMA_ON_START));
            }
            if (config.getBoolean(PURGE_ALL_ON_START) != null) {
                massIndexer.purgeAllOnStart(config.getBoolean(PURGE_ALL_ON_START));
            }
            if (config.getInteger(ID_FETCH_SIZE) != null) {
                massIndexer.idFetchSize(config.getInteger(ID_FETCH_SIZE));
            }
            if (config.getInteger(TRANSACTION_TIMEOUT) != null) {
                massIndexer.transactionTimeout(config.getInteger(TRANSACTION_TIMEOUT));
            }

            return massIndexer;
        }
    }
}
