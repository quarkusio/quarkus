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
    public void testIterateNames() {
        RestClientFallbackConfigSourceInterceptor interceptor = new RestClientFallbackConfigSourceInterceptor();
        Iterator<String> iterator;

        // client properties
        iterator = interceptor.iterateNames(new TestContext(Arrays.asList(
                "prefix/mp-rest/url",
                "a.b.c/mp-rest/url",
                "quarkus.rest.client.multipart-post-encoder-mode")));

        assertThat(iteratorToCollection(iterator)).containsOnly(
                "prefix/mp-rest/url",
                "quarkus.rest-client.prefix.url",

                "a.b.c/mp-rest/url",
                "quarkus.rest-client.\"a.b.c\".url",

                "quarkus.rest.client.multipart-post-encoder-mode",
                "quarkus.rest-client.multipart-post-encoder-mode");
    }

    @Test
    public void testIterateNamesExcludeInactiveProfiles() {
        RestClientFallbackConfigSourceInterceptor interceptor = new RestClientFallbackConfigSourceInterceptor();
        Iterator<String> iterator;

        // client properties
        iterator = interceptor.iterateNames(new TestContext(Arrays.asList(
                "%prod.key/mp-rest/url",
                "%dev.key/mp-rest/url",
                "key/mp-rest/url",
                "%prod.quarkus.rest-client.key2.url",
                "%dev.quarkus.rest-client.key2.url",
                "quarkus.rest-client.key2.url")));

        assertThat(iteratorToCollection(iterator)).containsOnly(
                // all the original property names should be included
                "%prod.key/mp-rest/url",
                "%dev.key/mp-rest/url",
                "key/mp-rest/url",
                "%prod.quarkus.rest-client.key2.url",
                "%dev.quarkus.rest-client.key2.url",
                "quarkus.rest-client.key2.url",

                // only the conversion of "key/mp-rest/url" should be added
                "quarkus.rest-client.key.url");
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
                    .withConfigSourceOrdinal(name.startsWith("quarkus.rest-client.") ? 100 : 200)
                    .build();
        }

        @Override
        public ConfigValue restart(String name) {
            return proceed(name);
        }

        @Override
        public Iterator<String> iterateNames() {
            return names.iterator();
        }
    }
}
