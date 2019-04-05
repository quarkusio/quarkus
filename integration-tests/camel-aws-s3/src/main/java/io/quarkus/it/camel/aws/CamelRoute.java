package io.quarkus.it.camel.aws;

import org.apache.camel.builder.RouteBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("timer:quarkus?repeatCount=1")
                .setHeader("CamelAwsS3Key", constant("testquarkus"))
                .setBody(constant("Quarkus is great!"))
                .to("aws-s3://devvox1")
                .to("log:sf?showAll=true");

    }

}
