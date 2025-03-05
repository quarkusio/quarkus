package io.quarkus.micrometer.runtime.binder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.micrometer.runtime.config.runtime.HttpClientConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpServerConfig;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;

public class HttpBinderConfigurationTest {
    @Test
    public void testHttpServerMetricsIgnorePatterns() {
        HttpServerConfig httpServerConfig = Mockito.mock(HttpServerConfig.class);
        Mockito.doReturn(Optional.of(new ArrayList<>(Arrays.asList(" /item/.* ", " /oranges/.* "))))
                .when(httpServerConfig)
                .ignorePatterns();
        VertxConfig vertxConfig = Mockito.mock(VertxConfig.class);
        HttpClientConfig httpClientConfig = Mockito.mock(HttpClientConfig.class);
        HttpBinderConfiguration binderConfig = new HttpBinderConfiguration(
                true, false,
                httpServerConfig, httpClientConfig, vertxConfig);
        List<Pattern> ignorePatterns = binderConfig.getServerIgnorePatterns();

        Assertions.assertEquals(2, ignorePatterns.size());

        Pattern p = ignorePatterns.get(0);
        Assertions.assertEquals("/item/.*", p.pattern());
        Assertions.assertTrue(p.matcher("/item/123").matches());

        p = ignorePatterns.get(1);
        Assertions.assertEquals("/oranges/.*", p.pattern());
        Assertions.assertTrue(p.matcher("/oranges/123").matches());
    }

    @Test
    public void testHttpServerMetricsMatchPatterns() {
        HttpServerConfig httpServerConfig = Mockito.mock(HttpServerConfig.class);
        Mockito.doReturn(Optional
                .of(new ArrayList<>(Arrays.asList(" /item/\\d+=/item/{id} ", "  /msg/\\d+=/msg/{other} "))))
                .when(httpServerConfig).matchPatterns();
        HttpClientConfig httpClientConfig = Mockito.mock(HttpClientConfig.class);
        VertxConfig vertxConfig = Mockito.mock(VertxConfig.class);
        HttpBinderConfiguration binderConfig = new HttpBinderConfiguration(
                true, false,
                httpServerConfig, httpClientConfig, vertxConfig);
        Map<Pattern, String> matchPatterns = binderConfig.getServerMatchPatterns();

        Assertions.assertFalse(matchPatterns.isEmpty());
        Iterator<Map.Entry<Pattern, String>> i = matchPatterns.entrySet().iterator();
        Map.Entry<Pattern, String> entry = i.next();

        Assertions.assertEquals("/item/\\d+", entry.getKey().pattern());
        Assertions.assertEquals("/item/{id}", entry.getValue());
        Assertions.assertTrue(entry.getKey().matcher("/item/123").matches());

        entry = i.next();
        Assertions.assertEquals("/msg/\\d+", entry.getKey().pattern());
        Assertions.assertEquals("/msg/{other}", entry.getValue());
        Assertions.assertTrue(entry.getKey().matcher("/msg/789").matches());
    }
}
