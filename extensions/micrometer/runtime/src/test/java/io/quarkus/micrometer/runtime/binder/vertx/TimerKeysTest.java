package io.quarkus.micrometer.runtime.binder.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.http.Outcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.micrometer.runtime.binder.HttpCommonTags;
import io.quarkus.micrometer.runtime.binder.MeterCache;

/**
 * The cache keys of the HTTP timers must identify exactly one set of tags.
 */
class TimerKeysTest {

    @Test
    void clientKeyHasExactlyTheTimerTags() {
        RestClientMetricsFilter.TimerKey key = new RestClientMetricsFilter.TimerKey(Tag.of("address", "host:8080"),
                HttpCommonTags.method("GET"), Tag.of("uri", "/a/{id}"), HttpCommonTags.outcome(200),
                HttpCommonTags.status(200), Tag.of("clientName", "host"));

        assertThat(key.tags()).containsExactlyInAnyOrder(Tag.of("address", "host:8080"), Tag.of("method", "GET"),
                Tag.of("uri", "/a/{id}"), Tag.of("outcome", "SUCCESS"), Tag.of("status", "200"),
                Tag.of("clientName", "host"));
        // equal tags, built separately, give an equal key
        assertThat(key).isEqualTo(new RestClientMetricsFilter.TimerKey(Tag.of("address", "host:8080"),
                Tag.of("method", "GET"), Tag.of("uri", "/a/{id}"), Tag.of("outcome", "SUCCESS"), Tag.of("status", "200"),
                Tag.of("clientName", "host")));
        assertThat(key).isNotEqualTo(new RestClientMetricsFilter.TimerKey(Tag.of("address", "host:8080"),
                Tag.of("method", "GET"), Tag.of("uri", "/a/{id}"), Tag.of("outcome", "SUCCESS"), Tag.of("status", "201"),
                Tag.of("clientName", "host")));
    }

    @Test
    void serverKeyHasExactlyTheTimerTags() {
        VertxHttpServerMetrics.RequestTimerKey key = new VertxHttpServerMetrics.RequestTimerKey(
                HttpCommonTags.method("GET"), Tag.of("uri", "/a/{id}"), Outcome.SUCCESS.asTag(),
                HttpCommonTags.status(200));

        assertThat(key.tags()).containsExactlyInAnyOrder(Tag.of("method", "GET"), Tag.of("uri", "/a/{id}"),
                Tag.of("outcome", "SUCCESS"), Tag.of("status", "200"));
        assertThat(key).isEqualTo(new VertxHttpServerMetrics.RequestTimerKey(Tag.of("method", "GET"),
                Tag.of("uri", "/a/{id}"), Tag.of("outcome", "SUCCESS"), Tag.of("status", "200")));
    }

    @Test
    void contributedTagsOverridingTheUriShareOneCacheEntry() {
        // a contributor replacing the raw uri by a constant, to reduce cardinality
        Tags first = Tags.of(Tag.of("method", "GET"), Tag.of("uri", "/users/1"), Tag.of("status", "200"))
                .and(Tags.of("uri", "/users"));
        Tags second = Tags.of(Tag.of("method", "GET"), Tag.of("uri", "/users/2"), Tag.of("status", "200"))
                .and(Tags.of("uri", "/users"));

        // the keys are equal, so they are a single cache entry
        assertThat(new VertxHttpServerMetrics.EffectiveTagsKey(first))
                .isEqualTo(new VertxHttpServerMetrics.EffectiveTagsKey(second));

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterCache<Timer> cache = new MeterCache<>(Timer.builder("test.server").withRegistry(registry));
        Timer timer1 = cache.get(new VertxHttpServerMetrics.EffectiveTagsKey(first));
        Timer timer2 = cache.get(new VertxHttpServerMetrics.EffectiveTagsKey(second));

        assertThat(timer2).isSameAs(timer1);
        assertThat(timer1.getId().getTag("uri")).isEqualTo("/users");
        assertThat(registry.find("test.server").timers()).hasSize(1);
    }
}
