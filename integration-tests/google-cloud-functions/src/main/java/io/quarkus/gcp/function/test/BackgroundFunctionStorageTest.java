package io.quarkus.gcp.function.test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;

import io.quarkus.gcp.function.test.service.GreetingService;

@Named("storageTest")
@ApplicationScoped
public class BackgroundFunctionStorageTest implements BackgroundFunction<BackgroundFunctionStorageTest.StorageEvent> {

    @Inject
    GreetingService greetingService;

    @Override
    public void accept(StorageEvent event, Context context) throws Exception {
        System.out.println("Receive event on file: " + event.name);
        System.out.println("Be polite, say " + greetingService.hello());
    }

    public static class StorageEvent {
        public String name;
    }
}
