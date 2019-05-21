package io.quarkus.it.camel.servlet;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {

        rest()
                .get("/rest-get")
                .route()
                .setBody(constant("GET: /rest-get"))
                .endRest()
                .post("/rest-post")
                .route()
                .setBody(constant("POST: /rest-post"))
                .endRest();

        from("servlet://hello?matchOnUriPrefix=true")
                .setBody(constant("GET: /hello"));

        from("servlet://custom?servletName=my-named-servlet")
                .setBody(constant("GET: /custom"));

        from("servlet://favorite?servletName=my-favorite-servlet")
                .setBody(constant("GET: /favorite"));

    }

}
