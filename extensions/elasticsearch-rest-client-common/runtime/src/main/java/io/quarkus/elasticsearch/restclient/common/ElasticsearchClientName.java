package io.quarkus.elasticsearch.restclient.common;

import io.quarkus.elasticsearch.restclient.common.runtime.ElasticsearchClientBeanUtil;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * CDI qualifier to select a particular Elasticsearch client by name.
 *
 * For example, if an Elasticsearch connection is configured like so in {@code application.properties}:
 *
 * <pre>
 * quarkus.elasticsearch.cluster1.uris=https://es1.mycompany.com:9200
 * </pre>
 *
 * Then to inject the proper {@code RestClient}, you would need to use {@code ElasticsearchClientName} like so:
 *
 * <pre>
 *     &#64Inject
 *     &#64ElasticsearchClientName("cluster1")
 *     RestClient client;
 * </pre>
 */
@Qualifier
@Target({ FIELD, TYPE, PARAMETER })
@Retention(RUNTIME)
public @interface ElasticsearchClientName {

    String DEFAULT = ElasticsearchClientBeanUtil.DEFAULT_ELASTICSEARCH_CLIENT_NAME;

    String value();

    class Literal extends AnnotationLiteral<ElasticsearchClientName> implements ElasticsearchClientName {

        private String name;

        public Literal(String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return name;
        }

    }
}
