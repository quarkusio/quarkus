package io.quarkus.spring.web.runtime;

import java.util.List;

import javax.ws.rs.core.Variant;

import org.jboss.resteasy.reactive.server.core.request.ServerDrivenNegotiation;

import io.quarkus.spring.web.runtime.common.AbstractResponseContentTypeResolver;

@SuppressWarnings("unused")
public class ResteasyReactiveResponseContentTypeResolver extends AbstractResponseContentTypeResolver {

    @Override
    protected Variant negotiateBestMatch(List<String> acceptHeaders, List<Variant> variants) {
        ServerDrivenNegotiation negotiation = new ServerDrivenNegotiation();
        negotiation.setAcceptHeaders(acceptHeaders);

        return negotiation.getBestMatch(variants);
    }
}
