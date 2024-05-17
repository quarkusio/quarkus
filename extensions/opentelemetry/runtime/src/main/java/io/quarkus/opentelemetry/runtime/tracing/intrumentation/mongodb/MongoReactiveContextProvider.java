package io.quarkus.opentelemetry.runtime.tracing.intrumentation.mongodb;

import org.reactivestreams.Subscriber;

import com.mongodb.RequestContext;
import com.mongodb.reactivestreams.client.ReactiveContextProvider;

public class MongoReactiveContextProvider implements ReactiveContextProvider {

    @Override
    public RequestContext getContext(Subscriber<?> subscriber) {
        return new MongoRequestContext();
    }

}
