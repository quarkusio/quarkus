package io.quarkus.it.qute;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;

@Path("/defaultmethod")
public class DefaultMethodResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance get() {
        return new hello(new Name(), LocalDateTime.now(), ZoneOffset.UTC);
    }

    record hello(Name name, LocalDateTime time, ZoneOffset zoneOffset) implements TemplateInstance {
    };

    public static class Name implements Something {

        public String name() {
            return "M";
        }
    }

    public interface Something {

        String name();

        default String fullName() {
            return name() + "K";
        }
    }

}
