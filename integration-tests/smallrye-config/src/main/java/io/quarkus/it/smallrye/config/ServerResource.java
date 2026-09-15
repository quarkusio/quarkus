package io.quarkus.it.smallrye.config;

import static io.smallrye.config.ConfigInstanceBuilder.forInterface;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.it.smallrye.config.Server.Cors;
import io.quarkus.it.smallrye.config.Server.Form;
import io.quarkus.it.smallrye.config.Server.Log;

@Path("/server")
public class ServerResource {
    @Inject
    Server server;
    @Inject
    @ConfigProperties
    ServerProperties serverProperties;
    @Inject
    @ConfigProperty(name = "server.info.message")
    Instance<String> message;
    @Inject
    @ConfigProperty(name = "http.server.form.positions")
    List<Integer> positions;

    @GET
    public Response getServer() {
        return Response.ok(server).build();
    }

    @GET
    @Path("/properties")
    public Response getServerProperties() {
        return Response.ok(serverProperties).build();
    }

    @GET
    @Path("/info")
    public String info() {
        return message.get();
    }

    @GET
    @Path("/positions")
    public List<Integer> positions() {
        return positions;
    }

    @GET
    @Path("/instance")
    public Response getServerInstance() {
        Server server = forInterface(Server.class)
                .withOptional(Server::name, "server")
                .withOptional(Server::alias, "server")
                .with(Server::host, "localhost")
                .with(Server::port, 8080)
                .with(Server::timeout, Duration.ofSeconds(60))
                .with(Server::threads, 200)
                .with(Server::bytes, "dummy".getBytes(StandardCharsets.UTF_8))
                .with(Server::form, Map.of("form", forInterface(Form.class)
                        .with(Form::loginPage, "login.html")
                        .with(Form::errorPage, "error.html")
                        .with(Form::landingPage, "index.html")
                        .with(Form::positions, List.of(10, 20))
                        .build()))
                .withOptional(Server::ssl, forInterface(Ssl.class)
                        .with(Ssl::port, 8443)
                        .with(Ssl::certificate, "certificate")
                        .build())
                .withOptional(Server::cors, forInterface(Cors.class)
                        .with(Cors::methods, List.of("GET", "POST"))
                        .with(Cors::origins, List.of(
                                forInterface(Cors.Origin.class)
                                        .with(Cors.Origin::host, "some-server")
                                        .with(Cors.Origin::port, 9000)
                                        .build(),
                                forInterface(Cors.Origin.class)
                                        .with(Cors.Origin::host, "another-server")
                                        .with(Cors.Origin::port, 8000)
                                        .build()))
                        .build())
                .with(Server::log, forInterface(Log.class)
                        .with(Log::period, Period.ofDays(1))
                        .with(Log::days, 10)
                        .build())
                .build();
        return Response.ok(server).build();
    }
}
