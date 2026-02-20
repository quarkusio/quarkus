package io.quarkus.elasticsearch.restclient.lowlevel;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import io.quarkus.elasticsearch.restclient.common.runtime.ElasticsearchClientBeanUtil;

/**
 * Annotate implementations of {@code org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback} to provide further
 * configuration of injected Elasticsearch {@code RestClient} You may provide multiple implementations each annotated with
 * {@code ElasticsearchClientConfig} and configuration provided by each implementation will be applied in a randomly ordered
 * cascading manner
 */
@Qualifier
@Target({ FIELD, TYPE, METHOD, PARAMETER })
@Retention(RUNTIME)
@Repeatable(ElasticsearchClientConfig.List.class)
public @interface ElasticsearchClientConfig {

    String DEFAULT = ElasticsearchClientBeanUtil.DEFAULT_ELASTICSEARCH_CLIENT_NAME;

    /**
     * @return The name of the REST client this config is applicable to.
     */
    String value() default DEFAULT;

    class Literal extends AnnotationLiteral<ElasticsearchClientConfig> implements ElasticsearchClientConfig {

        private final String name;

        public Literal() {
            this(DEFAULT);
        }

        public Literal(String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return name;
        }

    }

    @Target({ TYPE, FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    @Documented
    @interface List {
        ElasticsearchClientConfig[] value();
    }
}
