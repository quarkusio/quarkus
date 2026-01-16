package io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.graal;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchResponse;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

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
