package io.quarkus.restclient.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

public class RestClientFallbackConfigSourceInterceptorTest {

    @Test
    public void testExtractQuarkusClientPrefixAndProperty() {
        String[] prefixAndProperty;

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractQuarkusClientPrefixAndProperty("prefix.url");
        assertThat(prefixAndProperty[0]).isEqualTo("prefix");
        assertThat(prefixAndProperty[1]).isEqualTo("url");

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractQuarkusClientPrefixAndProperty("url");
        assertThat(prefixAndProperty[0]).isEqualTo(null);
        assertThat(prefixAndProperty[1]).isEqualTo("url");

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractQuarkusClientPrefixAndProperty("prefix.");
        assertThat(prefixAndProperty[0]).isEqualTo("prefix");
        assertThat(prefixAndProperty[1]).isEqualTo("");

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractQuarkusClientPrefixAndProperty("\"prefix\".url");
        assertThat(prefixAndProperty[0]).isEqualTo("\"prefix\"");
        assertThat(prefixAndProperty[1]).isEqualTo("url");

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractQuarkusClientPrefixAndProperty("\"a.b.c\".url");
        assertThat(prefixAndProperty[0]).isEqualTo("\"a.b.c\"");
        assertThat(prefixAndProperty[1]).isEqualTo("url");

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractQuarkusClientPrefixAndProperty("\"a.b.c\"");
        assertThat(prefixAndProperty[0]).isEqualTo(null);
        assertThat(prefixAndProperty[1]).isEqualTo("\"a.b.c\"");

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractQuarkusClientPrefixAndProperty("\"a.b.c");
        assertThat(prefixAndProperty[0]).isEqualTo(null);
        assertThat(prefixAndProperty[1]).isEqualTo("\"a.b.c");
    }

    @Test
    public void testExtractMPClientPrefixAndProperty() {
        String[] prefixAndProperty;

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractMPClientPrefixAndProperty("prefix/mp-rest/url");
        assertThat(prefixAndProperty).isNotNull();
        assertThat(prefixAndProperty[0]).isEqualTo("prefix");
        assertThat(prefixAndProperty[1]).isEqualTo("url");

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractMPClientPrefixAndProperty("a.b.c/mp-rest/url");
        assertThat(prefixAndProperty).isNotNull();
        assertThat(prefixAndProperty[0]).isEqualTo("\"a.b.c\"");
        assertThat(prefixAndProperty[1]).isEqualTo("url");

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractMPClientPrefixAndProperty("a.b.c/mp-rest/");
        assertThat(prefixAndProperty).isNotNull();
        assertThat(prefixAndProperty[0]).isEqualTo("\"a.b.c\"");
        assertThat(prefixAndProperty[1]).isEqualTo("");

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractMPClientPrefixAndProperty("/mp-rest/");
        assertThat(prefixAndProperty).isNull();

        prefixAndProperty = RestClientFallbackConfigSourceInterceptor
                .extractMPClientPrefixAndProperty("not-mp-rest");
        assertThat(prefixAndProperty).isNull();
    }

    @Test
    public void testInterceptorGetValue() {
        RestClientFallbackConfigSourceInterceptor interceptor = new RestClientFallbackConfigSourceInterceptor();
        ConfigSourceInterceptorContext interceptorContext = new TestContext();
        ConfigValue value;

        // client properties
        value = interceptor.getValue(interceptorContext, "quarkus.rest-client.prefix.url");
        assertThat(value.getName()).isEqualTo("prefix/mp-rest/url");

        value = interceptor.getValue(interceptorContext, "quarkus.rest-client.\"a.b.c\".url");
        assertThat(value.getName()).isEqualTo("a.b.c/mp-rest/url");

        // global properties
        value = interceptor.getValue(interceptorContext, "quarkus.rest-client.multipart-post-encoder-mode");
        assertThat(value.getName()).isEqualTo("quarkus.rest.client.multipart-post-encoder-mode");

        value = interceptor.getValue(interceptorContext, "quarkus.rest-client.disable-smart-produces");
        assertThat(value.getName()).isEqualTo("quarkus.rest-client-reactive.disable-smart-produces");

        // special cases
        value = interceptor.getValue(interceptorContext, "quarkus.rest-client.prefix.max-redirects");
        assertThat(value.getName()).isEqualTo("quarkus.rest.client.max-redirects");
    }

    @Test
    public void testInterceptorIterateNames() {
        RestClientFallbackConfigSourceInterceptor interceptor = new RestClientFallbackConfigSourceInterceptor();
        Iterator<String> iterator;

        // client properties
        iterator = interceptor.iterateNames(new TestContext(Arrays.asList(
                "prefix/mp-rest/url",
                "a.b.c/mp-rest/url",
                "quarkus.rest.client.multipart-post-encoder-mode",
                "quarkus.rest-client-reactive.disable-smart-produces")));

        assertThat(iteratorToCollection(iterator)).containsOnly(
                "prefix/mp-rest/url",
                "quarkus.rest-client.prefix.url",

                "a.b.c/mp-rest/url",
                "quarkus.rest-client.\"a.b.c\".url",

                "quarkus.rest.client.multipart-post-encoder-mode",
                "quarkus.rest-client.multipart-post-encoder-mode",

                "quarkus.rest-client-reactive.disable-smart-produces",
                "quarkus.rest-client.disable-smart-produces");
    }

    private static Collection<String> iteratorToCollection(Iterator<String> iterator) {
        ArrayList<String> list = new ArrayList<>();
        iterator.forEachRemaining(list::add);
        return list;
    }

    private static class TestContext implements ConfigSourceInterceptorContext {

        private final Collection<String> names;

        public TestContext() {
            names = Collections.emptyList();
        }

        public TestContext(Collection<String> names) {
            this.names = names;
        }

        @Override
        public ConfigValue proceed(String name) {
            return ConfigValue.builder()
                    .withName(name)
                    .withValue("")
                    .build();
        }

        @Override
        public Iterator<String> iterateNames() {
            return names.iterator();
        }

        @Override
        public Iterator<ConfigValue> iterateValues() {
            return null;
        }
    }

}
