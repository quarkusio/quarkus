package io.quarkus.gcp.function.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.google.cloud.functions.CloudEventsFunction;

import io.cloudevents.CloudEvent;
import io.quarkus.gcp.function.test.service.GreetingService;

@Named("cloudEventTest")
@ApplicationScoped
public class CloudEventStorageTest implements CloudEventsFunction {
    @Inject
    GreetingService greetingService;

    @Override
    public void accept(CloudEvent cloudEvent) throws Exception {
        System.out.println("Receive event Id: " + cloudEvent.getId());
        System.out.println("Receive event Subject: " + cloudEvent.getSubject());
        System.out.println("Receive event Type: " + cloudEvent.getType());
        System.out.println("Receive event Data: " + new String(cloudEvent.getData().toBytes()));
        System.out.println("Be polite, say " + greetingService.hello());
    }
}
