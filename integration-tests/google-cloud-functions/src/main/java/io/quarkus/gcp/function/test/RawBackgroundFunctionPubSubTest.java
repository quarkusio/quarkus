package io.quarkus.gcp.function.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.google.cloud.functions.Context;
import com.google.cloud.functions.RawBackgroundFunction;

import io.quarkus.gcp.function.test.service.GreetingService;

@Named("rawPubSubTest")
@ApplicationScoped
public class RawBackgroundFunctionPubSubTest implements RawBackgroundFunction {
    @Inject
    GreetingService greetingService;

    @Override
    public void accept(String event, Context context) throws Exception {
        System.out.println("PubSub event: " + event);
        System.out.println("Be polite, say " + greetingService.hello());
    }
}
