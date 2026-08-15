package io.quarkus.elasticsearch.restclient.lowlevel;

import java.util.function.Consumer;

import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;

/**
 * Allows customization of the underlying HTTP client used to connect to Elasticsearch.
 */
public interface ElasticsearchClientConfigConfigurer extends Consumer<HttpAsyncClientBuilder> {

}
