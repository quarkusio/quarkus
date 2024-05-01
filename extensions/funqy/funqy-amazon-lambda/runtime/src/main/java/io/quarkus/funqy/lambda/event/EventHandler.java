package io.quarkus.funqy.lambda.event;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.quarkus.funqy.lambda.config.FunqyAmazonConfig;

public interface EventHandler<E, M, R> {
    Stream<M> streamEvent(E event, FunqyAmazonConfig amazonConfig);

    String getIdentifier(M message, FunqyAmazonConfig amazonConfig);

    Supplier<InputStream> getBody(M message, FunqyAmazonConfig amazonConfig);

    R createResponse(List<String> failures, FunqyAmazonConfig amazonConfig);

    Class<M> getMessageClass();
}
