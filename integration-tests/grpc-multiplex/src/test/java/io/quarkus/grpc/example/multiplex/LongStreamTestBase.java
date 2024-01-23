package io.quarkus.grpc.example.multiplex;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.grpc.examples.multiplex.LongReply;
import io.grpc.examples.multiplex.Multiplex;
import io.grpc.examples.multiplex.StringRequest;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

@SuppressWarnings("NewClassNamingConvention")
public class LongStreamTestBase {
    @GrpcClient("streaming")
    Multiplex multiplex;

    @Test
    @Timeout(10)
    public void testParse() {
        Multi<StringRequest> multi = Multi.createFrom().range(1, 10)
                .map(x -> StringRequest.newBuilder()
                        .setNumber(x.toString())
                        .build());

        AssertSubscriber<LongReply> subscriber = multiplex.parse(multi)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        Set<Long> longSet = subscriber.awaitCompletion()
                .getItems()
                .stream()
                .map(LongReply::getValue)
                .collect(Collectors.toSet());

        Set<Long> expected = LongStream.range(1, 10).boxed().collect(Collectors.toSet());
        Assertions.assertEquals(expected, longSet);
    }
}
