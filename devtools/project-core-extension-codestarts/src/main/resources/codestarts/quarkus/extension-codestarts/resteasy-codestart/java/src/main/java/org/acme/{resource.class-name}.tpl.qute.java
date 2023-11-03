package org.acme;

{#if quarkus.bom.version.startsWith("2.") or quarkus.bom.version.startsWith("1.")}
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
{#else}
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
{/if}

@Path("{resource.path}")
public class {resource.class-name} {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "{resource.response}";
    }
}
