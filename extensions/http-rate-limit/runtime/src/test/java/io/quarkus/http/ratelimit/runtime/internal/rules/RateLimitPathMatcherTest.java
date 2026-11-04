package io.quarkus.http.ratelimit.runtime.internal.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateLimitPathMatcherTest {

    @Test
    void everyOverlappingPatternMatches() {
        RateLimitPathMatcher<String> matcher = RateLimitPathMatcher.<String> builder()
                .add("/*", "G")
                .add("/api/*", "A")
                .add("/api/users", "B")
                .add("/login", "C")
                .build();

        assertThat(matcher.match("/api/users", "/api/users")).containsExactly("G", "A", "B");
        assertThat(matcher.match("/api/x/y", "/api/x/y")).containsExactly("G", "A");
        assertThat(matcher.match("/login", "/login")).containsExactly("G", "C");
        assertThat(matcher.match("/other", "/other")).containsExactly("G");
    }

    @Test
    void sameValueThroughTwoPatternsIsReturnedOnce() {
        RateLimitPathMatcher<String> matcher = RateLimitPathMatcher.<String> builder()
                .add("/api/*", "A")
                .add("/api/users", "A")
                .build();

        assertThat(matcher.match("/api/users", "/api/users")).containsExactly("A");
    }

    @Test
    void innerWildcardMatchesOneSegment() {
        RateLimitPathMatcher<String> matcher = RateLimitPathMatcher.<String> builder()
                .add("/users/*/orders", "D")
                .build();

        assertThat(matcher.match("/users/42/orders", "/users/42/orders")).containsExactly("D");
        assertThat(matcher.match("/users/42/x/orders", "/users/42/x/orders")).isEmpty();
    }

    @Test
    void valuesOfEitherPathMatchInInsertionOrder() {
        RateLimitPathMatcher<String> matcher = RateLimitPathMatcher.<String> builder()
                .add("/*", "G")
                .add("/api/*", "A")
                .add("/public", "P")
                .build();

        assertThat(matcher.match("/public", "/api/items/..%2F..%2Fpublic")).containsExactly("G", "A", "P");
        assertThat(matcher.match("/public", "/public")).containsExactly("G", "P");
    }
}
