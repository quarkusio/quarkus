package io.quarkus.camel.it.salesforce;

import org.apache.camel.builder.RouteBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:case")
                .autoStartup(false)
                .setHeader("sObjectName").constant("Case")
                .to("salesforce:getSObject?rawPayload=true")
                .to("log:sf?showAll=true");

    }
}
