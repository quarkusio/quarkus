package ilove.quark.us.funqygooglecloudfunctions;

import javax.inject.Inject;

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

}
