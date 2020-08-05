package io.quarkus.qrs.runtime.jaxrs;

import java.util.Date;
import java.util.Locale;

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

import io.quarkus.qrs.runtime.headers.CacheControlDelegate;
import io.quarkus.qrs.runtime.headers.CookieHeaderDelegate;
import io.quarkus.qrs.runtime.headers.DateDelegate;
import io.quarkus.qrs.runtime.headers.EntityTagDelegate;
import io.quarkus.qrs.runtime.headers.LocaleDelegate;
import io.quarkus.qrs.runtime.headers.MediaTypeHeaderDelegate;
import io.quarkus.qrs.runtime.headers.NewCookieHeaderDelegate;

public class QrsRuntimeDelegate extends RuntimeDelegate {

    @Override
    public UriBuilder createUriBuilder() {
        return new QrsUriBuilder();
    }

    @Override
    public Response.ResponseBuilder createResponseBuilder() {
        return new QrsResponseBuilder();
    }

    @Override
    public Variant.VariantListBuilder createVariantListBuilder() {
        return new QrsVariantListBuilder();
    }

    @Override
    public <T> T createEndpoint(Application application, Class<T> endpointType)
            throws IllegalArgumentException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) throws IllegalArgumentException {
        if (type.equals(MediaType.class)) {
            return new MediaTypeHeaderDelegate<T>();
        } else if (type.equals(Date.class)) {
            return (HeaderDelegate<T>) new DateDelegate();
        } else if (type.equals(CacheControl.class)) {
            return (HeaderDelegate<T>) new CacheControlDelegate();
        } else if (type.equals(NewCookie.class)) {
            return (HeaderDelegate<T>) new NewCookieHeaderDelegate();
        } else if (type.equals(Cookie.class)) {
            return (HeaderDelegate<T>) new CookieHeaderDelegate();
        } else if (type.equals(EntityTag.class)) {
            return (HeaderDelegate<T>) new EntityTagDelegate();
        } else if (type.equals(Locale.class)) {
            return (HeaderDelegate<T>) new LocaleDelegate();
        }
        throw new IllegalArgumentException();
    }

    @Override
    public Link.Builder createLinkBuilder() {
        return new QrsLinkBuilder();
    }
}
