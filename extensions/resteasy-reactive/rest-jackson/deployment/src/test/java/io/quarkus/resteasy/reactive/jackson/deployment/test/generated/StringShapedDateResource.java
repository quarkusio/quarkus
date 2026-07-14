package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.smallrye.common.annotation.NonBlocking;

@Path("/string-shaped-date")
@NonBlocking
public class StringShapedDateResource {

    @GET
    public StringShapedDateBean get() {
        StringShapedDateBean bean = new StringShapedDateBean();
        bean.setName("string-shaped-date");
        bean.setDate(Date.from(LocalDate.of(2025, 6, 15).atStartOfDay().toInstant(ZoneOffset.UTC)));
        return bean;
    }
}
