package org.jboss.resteasy.reactive.common.jaxrs;

import java.util.Date;
import java.util.Locale;
import java.util.ServiceLoader;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.RuntimeDelegate;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;
import org.jboss.resteasy.reactive.common.core.ResponseBuilderFactory;
import org.jboss.resteasy.reactive.common.headers.CacheControlDelegate;
import org.jboss.resteasy.reactive.common.headers.CookieHeaderDelegate;
import org.jboss.resteasy.reactive.common.headers.DateDelegate;
import org.jboss.resteasy.reactive.common.headers.EntityTagDelegate;
import org.jboss.resteasy.reactive.common.headers.LinkDelegate;
import org.jboss.resteasy.reactive.common.headers.LocaleDelegate;
import org.jboss.resteasy.reactive.common.headers.MediaTypeHeaderDelegate;
import org.jboss.resteasy.reactive.common.headers.NewCookieHeaderDelegate;
import org.jboss.resteasy.reactive.common.headers.ObjectToStringDelegate;

public class RuntimeDelegateImpl extends RuntimeDelegate {

    static final ResponseBuilderFactory factory;

    static {
        ResponseBuilderFactory result = new ResponseBuilderFactory() {
            @Override
            public Response.ResponseBuilder create() {
                throw new RuntimeException("Resteasy Reactive server side components are not installed.");
            }

            @Override
            public int priority() {
                return 0;
            }

            @Override
            public <T> ResponseBuilder<T> createRestResponse() {
                throw new RuntimeException("Resteasy Reactive server side components are not installed.");
            }
        };
        ServiceLoader<ResponseBuilderFactory> sl = ServiceLoader.load(ResponseBuilderFactory.class,
                RuntimeDelegateImpl.class.getClassLoader());
        for (ResponseBuilderFactory i : sl) {
            if (result.priority() < i.priority()) {
                result = i;
            }
        }
        factory = result;
    }

    @Override
    public UriBuilder createUriBuilder() {
        return new UriBuilderImpl();
    }

    @Override
    public Response.ResponseBuilder createResponseBuilder() {
        return factory.create();
    }

    public <T> RestResponse.ResponseBuilder<T> createRestResponseBuilder() {
        return factory.createRestResponse();
    }

    @Override
    public Variant.VariantListBuilder createVariantListBuilder() {
        return new VariantListBuilderImpl();
    }

    @Override
    public <T> T createEndpoint(Application application, Class<T> endpointType)
            throws IllegalArgumentException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) throws IllegalArgumentException {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (type.equals(MediaType.class)) {
            return (HeaderDelegate<T>) MediaTypeHeaderDelegate.INSTANCE;
        } else if (type.equals(Date.class)) {
            return (HeaderDelegate<T>) DateDelegate.INSTANCE;
        } else if (type.equals(CacheControl.class)) {
            return (HeaderDelegate<T>) CacheControlDelegate.INSTANCE;
        } else if (type.equals(NewCookie.class)) {
            return (HeaderDelegate<T>) NewCookieHeaderDelegate.INSTANCE;
        } else if (type.equals(Cookie.class)) {
            return (HeaderDelegate<T>) CookieHeaderDelegate.INSTANCE;
        } else if (type.equals(EntityTag.class)) {
            return (HeaderDelegate<T>) EntityTagDelegate.INSTANCE;
        } else if (type.equals(Locale.class)) {
            return (HeaderDelegate<T>) LocaleDelegate.INSTANCE;
        } else if (type.equals(Link.class)) {
            return (HeaderDelegate<T>) LinkDelegate.INSTANCE;
        } else {
            return (HeaderDelegate<T>) ObjectToStringDelegate.INSTANCE;
        }
    }

    @Override
    public Link.Builder createLinkBuilder() {
        return new LinkBuilderImpl();
    }
}
