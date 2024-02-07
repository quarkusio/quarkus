package io.quarkus.rest.client.reactive.deployment;

import java.lang.reflect.Method;

import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParams;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.annotation.RegisterProviders;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.client.SseEventFilter;

import io.quarkus.rest.client.reactive.ClientBasicAuth;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.quarkus.rest.client.reactive.ClientFormParam;
import io.quarkus.rest.client.reactive.ClientFormParams;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.quarkus.rest.client.reactive.ClientQueryParams;
import io.quarkus.rest.client.reactive.ClientRedirectHandler;

public class DotNames {

    public static final DotName REGISTER_PROVIDER = DotName.createSimple(RegisterProvider.class.getName());
    public static final DotName REGISTER_PROVIDERS = DotName.createSimple(RegisterProviders.class.getName());
    public static final DotName CLIENT_HEADER_PARAM = DotName.createSimple(ClientHeaderParam.class.getName());
    public static final DotName CLIENT_HEADER_PARAMS = DotName.createSimple(ClientHeaderParams.class.getName());

    public static final DotName CLIENT_QUERY_PARAM = DotName.createSimple(ClientQueryParam.class.getName());
    public static final DotName CLIENT_QUERY_PARAMS = DotName.createSimple(ClientQueryParams.class.getName());
    public static final DotName CLIENT_FORM_PARAM = DotName.createSimple(ClientFormParam.class.getName());
    public static final DotName CLIENT_FORM_PARAMS = DotName.createSimple(ClientFormParams.class.getName());
    public static final DotName REGISTER_CLIENT_HEADERS = DotName.createSimple(RegisterClientHeaders.class.getName());
    public static final DotName CLIENT_REQUEST_FILTER = DotName.createSimple(ClientRequestFilter.class.getName());
    public static final DotName CLIENT_RESPONSE_FILTER = DotName.createSimple(ClientResponseFilter.class.getName());
    public static final DotName CLIENT_EXCEPTION_MAPPER = DotName.createSimple(ClientExceptionMapper.class.getName());
    public static final DotName CLIENT_REDIRECT_HANDLER = DotName.createSimple(ClientRedirectHandler.class.getName());

    public static final DotName CLIENT_BASIC_AUTH = DotName.createSimple(ClientBasicAuth.class.getName());

    public static final DotName RESPONSE_EXCEPTION_MAPPER = DotName.createSimple(ResponseExceptionMapper.class.getName());

    static final DotName METHOD = DotName.createSimple(Method.class.getName());

    public static final DotName SSE_EVENT_FILTER = DotName.createSimple(SseEventFilter.class);

    private DotNames() {
    }
}
