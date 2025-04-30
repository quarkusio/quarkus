package io.quarkus.mongodb.runtime;

import org.reactivestreams.Subscriber;

import com.mongodb.RequestContext;
import com.mongodb.reactivestreams.client.ReactiveContextProvider;

import io.opentelemetry.context.Context;

public class MongoReactiveContextProvider implements ReactiveContextProvider {

    @Override
    public RequestContext getContext(Subscriber<?> subscriber) {
        return new MongoRequestContext(Context.current());
    }

}
