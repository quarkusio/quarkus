package io.quarkus.funqy.gcp.functions.test;

import jakarta.inject.Inject;

import io.cloudevents.CloudEvent;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.gcp.functions.event.PubsubMessage;
import io.quarkus.funqy.gcp.functions.event.StorageEvent;

public class GreetingFunctions {

    @Inject
    GreetingService service;

    @Funq
    public void helloPubSubWorld(PubsubMessage pubSubEvent) {
        String message = service.hello("world");
        System.out.println(pubSubEvent.messageId + " - " + message);
    }

    @Funq
    public void helloGCSWorld(StorageEvent storageEvent) {
        String message = service.hello("world");
        System.out.println(storageEvent.name + " - " + message);
    }

    @Funq
    public void helloCloudEvent(CloudEvent cloudEvent) {
        System.out.println("Receive event Id: " + cloudEvent.getId());
        System.out.println("Receive event Subject: " + cloudEvent.getSubject());
        System.out.println("Receive event Type: " + cloudEvent.getType());
        System.out.println("Receive event Data: " + new String(cloudEvent.getData().toBytes()));
        System.out.println("Be polite, say " + service.hello("world"));
    }

}
