package io.quarkus.it.camel.jdbc;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("timer:jdbc?repeatCount=1")
                .setBody(constant("select species from camels where id = 1"))
                .to("jdbc:camelsDs")
                .convertBodyTo(String.class)
                .to("file:target?fileName=out.txt");
    }

}
