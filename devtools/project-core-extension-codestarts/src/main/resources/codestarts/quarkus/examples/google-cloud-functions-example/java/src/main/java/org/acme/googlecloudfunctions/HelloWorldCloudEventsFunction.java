package org.acme.googlecloudfunctions;

import jakarta.enterprise.context.ApplicationScoped;

import com.google.cloud.functions.CloudEventsFunction;

import io.cloudevents.CloudEvent;

@ApplicationScoped
public class HelloWorldCloudEventsFunction implements CloudEventsFunction {

    @Override
    public void accept(CloudEvent cloudEvent) throws Exception {
        System.out.println("Receive event Id: " + cloudEvent.getId());
        System.out.println("Receive event Subject: " + cloudEvent.getSubject());
        System.out.println("Receive event Type: " + cloudEvent.getType());
        System.out.println("Receive event Data: " + new String(cloudEvent.getData().toBytes()));
    }
}
