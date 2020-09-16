package io.quarkus.grpc.server.devmode;

import java.time.Duration;

import javax.inject.Singleton;

import com.example.test.MutinyStreamsGrpc;
import com.example.test.StreamsOuterClass.Item;

import io.smallrye.mutiny.Multi;

@Singleton
public class DevModeTestStreamService extends MutinyStreamsGrpc.StreamsImplBase {

    public static final String PREFIX = "echo::";

    @Override
    public Multi<Item> echo(Multi<Item> request) {
        return request.flatMap(value -> Multi.createFrom().ticks().every(Duration.ofMillis(20))
                .map(whatever -> Item.newBuilder().setName(PREFIX + value.getName()).build()));
    }
}
