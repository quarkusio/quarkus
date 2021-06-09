package io.quarkus.rest.client.reactive.deployment;

import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParams;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.annotation.RegisterProviders;
import org.jboss.jandex.DotName;

public class DotNames {

    public static final DotName REGISTER_PROVIDER = DotName.createSimple(RegisterProvider.class.getName());
    public static final DotName REGISTER_PROVIDERS = DotName.createSimple(RegisterProviders.class.getName());
    public static final DotName CLIENT_HEADER_PARAM = DotName.createSimple(ClientHeaderParam.class.getName());
    public static final DotName CLIENT_HEADER_PARAMS = DotName.createSimple(ClientHeaderParams.class.getName());
    public static final DotName REGISTER_CLIENT_HEADERS = DotName.createSimple(RegisterClientHeaders.class.getName());
    public static final DotName CLIENT_REQUEST_FILTER = DotName.createSimple(ClientRequestFilter.class.getName());
    public static final DotName CLIENT_RESPONSE_FILTER = DotName.createSimple(ClientResponseFilter.class.getName());

    private DotNames() {
    }
}
