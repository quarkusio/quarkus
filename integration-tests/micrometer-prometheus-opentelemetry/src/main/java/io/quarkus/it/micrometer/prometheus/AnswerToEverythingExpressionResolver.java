package io.quarkus.it.micrometer.prometheus;

import jakarta.inject.Singleton;

import io.micrometer.common.annotation.ValueExpressionResolver;

@Singleton
public class AnswerToEverythingExpressionResolver implements ValueExpressionResolver {
    @Override
    public String resolve(String expression, Object parameter) {
        // Answer to the Ultimate Question of Life, the Universe, and Everything.
        return "42";
    }
}
