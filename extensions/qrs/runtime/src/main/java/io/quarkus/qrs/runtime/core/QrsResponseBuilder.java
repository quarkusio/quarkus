package io.quarkus.qrs.runtime.core;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;


public class QrsResponseBuilder extends ResponseBuilder {

    private int status;
    private String reasonPhrase;
    private Object entity;
    private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

    @Override
    public Response build() {
        QrsResponse response = new QrsResponse();
        response.status = status;
        response.reasonPhrase = reasonPhrase;
        response.entity = entity;
        response.headers = new MultivaluedHashMap<>(headers);
        return response;
    }

    @Override
    public ResponseBuilder clone() {
        QrsResponseBuilder responseBuilder = new QrsResponseBuilder();
        responseBuilder.status = status;
        responseBuilder.reasonPhrase = reasonPhrase;
        responseBuilder.entity = entity;
        responseBuilder.headers = new MultivaluedHashMap<>(headers);
        return responseBuilder;
    }

    @Override
    public ResponseBuilder status(int status) {
        this.status = status;
        return this;
    }

    @Override
    public ResponseBuilder status(int status, String reasonPhrase) {
        this.status = status;
        this.reasonPhrase = reasonPhrase;
        return this;
    }

    @Override
    public ResponseBuilder entity(Object entity) {
        this.entity = entity;
        return this;
    }

    @Override
    public ResponseBuilder entity(Object entity, Annotation[] annotations) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder allow(String... methods) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder allow(Set<String> methods) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder cacheControl(CacheControl cacheControl) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder encoding(String encoding) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder header(String name, Object value) {
        this.headers.add(name, value);
        return this;
    }

    @Override
    public ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
        this.headers.clear();
        this.headers.putAll(headers);
        return this;
    }

    @Override
    public ResponseBuilder language(String language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder language(Locale language) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder type(MediaType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder type(String type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder variant(Variant variant) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder contentLocation(URI location) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder cookie(NewCookie... cookies) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder expires(Date expires) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder lastModified(Date lastModified) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder location(URI location) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder tag(EntityTag tag) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder tag(String tag) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder variants(Variant... variants) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder variants(List<Variant> variants) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder links(Link... links) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder link(URI uri, String rel) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseBuilder link(String uri, String rel) {
        // TODO Auto-generated method stub
        return null;
    }

}
