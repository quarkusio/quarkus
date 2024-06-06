package io.quarkus.mongodb;

import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Subscriber;

import com.mongodb.RequestContext;
import com.mongodb.reactivestreams.client.ReactiveContextProvider;

import io.quarkus.mongodb.runtime.MongoRequestContext;

public class MockReactiveContextProvider implements ReactiveContextProvider {

    public static final List<String> EVENTS = new ArrayList<>();

    @Override
    public RequestContext getContext(Subscriber<?> subscriber) {
        EVENTS.add(MongoRequestContext.class.getName());
        return new MongoRequestContext(null);
    }
}
