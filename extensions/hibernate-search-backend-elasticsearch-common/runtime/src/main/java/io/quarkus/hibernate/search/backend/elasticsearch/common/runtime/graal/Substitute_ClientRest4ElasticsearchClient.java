package io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.graal;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchResponse;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

// The Elasticsearch backend in Hibernate Search comes with a transitive dependency on the Apache 4 based client.
// We don't need it at runtime but GraalVM would see it as potentially reachable and would fail with missing classes.
// Since we don't need the Apache 4 client here, we just make it a failing impl.
// (ClientRest4ElasticsearchClient should never be used at runtime).
// TODO: With Apache 4 client removed from dependencies of the backend, this would go away.
@TargetClass(className = "org.hibernate.search.backend.elasticsearch.client.impl.ClientRest4ElasticsearchClient")
final class Substitute_ClientRest4ElasticsearchClient {

    @Substitute
    public CompletableFuture<ElasticsearchResponse> submit(ElasticsearchRequest elasticsearchRequest) {
        throw new UnsupportedOperationException();
    }

    @Substitute
    public <T> T unwrap(Class<T> aClass) {
        throw new UnsupportedOperationException();
    }
}
