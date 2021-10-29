package io.quarkus.spring.web.runtime;

import java.util.List;

import javax.ws.rs.core.Variant;

import org.jboss.resteasy.core.request.ServerDrivenNegotiation;

import io.quarkus.spring.web.runtime.common.AbstractResponseContentTypeResolver;

@SuppressWarnings("unused")
public class ResteasyClassicResponseContentTypeResolver extends AbstractResponseContentTypeResolver {

    @Override
    protected Variant negotiateBestMatch(List<String> acceptHeaders, List<Variant> variants) {
        ServerDrivenNegotiation negotiation = new ServerDrivenNegotiation();
        negotiation.setAcceptHeaders(acceptHeaders);

        return negotiation.getBestMatch(variants);
    }
}
