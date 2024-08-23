package io.quarkus.elasticsearch.restclient.lowlevel;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Annotate implementations of {@code org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback} to provide further
 * configuration of injected Elasticsearch {@code RestClient} You may provide multiple implementations each annotated with
 * {@code ElasticsearchClientConfig} and configuration provided by each implementation will be applied in a randomly ordered
 * cascading manner
 */
@Qualifier
@Target({ FIELD, TYPE, PARAMETER })
@Retention(RUNTIME)
public @interface ElasticsearchClientConfig {

    class Literal extends AnnotationLiteral<ElasticsearchClientConfig> implements ElasticsearchClientConfig {

    }
}
