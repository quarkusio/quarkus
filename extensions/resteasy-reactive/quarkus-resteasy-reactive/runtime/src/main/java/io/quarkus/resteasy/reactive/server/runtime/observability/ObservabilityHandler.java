package io.quarkus.resteasy.reactive.server.runtime.observability;

import static io.quarkus.resteasy.reactive.server.runtime.observability.ObservabilityUtil.*;

import java.util.regex.Pattern;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.vertx.ext.web.RoutingContext;

public class ObservabilityHandler implements ServerRestHandler {

    static final Pattern MULTIPLE_SLASH_PATTERN = Pattern.compile("//+");

    // make mutable to allow for bytecode serialization
    private String templatePath;

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = MULTIPLE_SLASH_PATTERN.matcher(templatePath).replaceAll("/");
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        setUrlPathTemplate(requestContext.unwrap(RoutingContext.class), templatePath);
    }

}
