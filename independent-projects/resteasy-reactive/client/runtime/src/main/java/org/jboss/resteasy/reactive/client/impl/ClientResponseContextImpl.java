package org.jboss.resteasy.reactive.client.impl;

import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Link.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response.StatusType;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.common.headers.LinkHeaders;
import org.jboss.resteasy.reactive.common.jaxrs.StatusTypeImpl;

public class ClientResponseContextImpl implements ClientResponseContext {

    private final RestClientRequestContext state;

    public ClientResponseContextImpl(RestClientRequestContext state) {
        this.state = state;
    }

    public String getReasonPhrase() {
        return state.getResponseReasonPhrase();
    }

    public ClientResponseContextImpl setReasonPhrase(String reasonPhrase) {
        state.setResponseReasonPhrase(reasonPhrase);
        return this;
    }

    public ClientResponseContextImpl setHeaders(MultivaluedMap<String, String> headers) {
        state.setResponseHeaders(headers);
        return this;
    }

    @Override
    public int getStatus() {
        return state.getResponseStatus();
    }

    @Override
    public void setStatus(int code) {
        state.setResponseStatus(code);
    }

    @Override
    public StatusType getStatusInfo() {
        return new StatusTypeImpl(state.getResponseStatus(), state.getResponseReasonPhrase());
    }

    @Override
    public void setStatusInfo(StatusType statusInfo) {
        state.setResponseStatus(statusInfo.getStatusCode())
                .setResponseReasonPhrase(statusInfo.getReasonPhrase());
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return state.getResponseHeaders();
    }

    @Override
    public String getHeaderString(String name) {
        return HeaderUtil.getHeaderString(state.getResponseHeaders(), name);
    }

    @Override
    public Set<String> getAllowedMethods() {
        return HeaderUtil.getAllowedMethods(state.getResponseHeaders());
    }

    @Override
    public Date getDate() {
        return HeaderUtil.getDate(state.getResponseHeaders());
    }

    @Override
    public Locale getLanguage() {
        return HeaderUtil.getLanguage(state.getResponseHeaders());
    }

    @Override
    public int getLength() {
        return HeaderUtil.getLength(state.getResponseHeaders());
    }

    @Override
    public MediaType getMediaType() {
        return HeaderUtil.getMediaType(state.getResponseHeaders());
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return HeaderUtil.getNewCookies(state.getResponseHeaders());
    }

    @Override
    public EntityTag getEntityTag() {
        return HeaderUtil.getEntityTag(state.getResponseHeaders());
    }

    @Override
    public Date getLastModified() {
        return HeaderUtil.getLastModified(state.getResponseHeaders());
    }

    @Override
    public URI getLocation() {
        return HeaderUtil.getLocation(state.getResponseHeaders());
    }

    private LinkHeaders getLinkHeaders() {
        return new LinkHeaders((MultivaluedMap) state.getResponseHeaders());
    }

    @Override
    public Set<Link> getLinks() {
        return new HashSet<>(getLinkHeaders().getLinks());
    }

    @Override
    public boolean hasLink(String relation) {
        return getLinkHeaders().getLinkByRelationship(relation) != null;
    }

    @Override
    public Link getLink(String relation) {
        return getLinkHeaders().getLinkByRelationship(relation);
    }

    @Override
    public Builder getLinkBuilder(String relation) {
        Link link = getLinkHeaders().getLinkByRelationship(relation);
        if (link == null) {
            return null;
        }
        return Link.fromLink(link);
    }

    @Override
    public boolean hasEntity() {
        return state.getResponseEntityStream() != null;
    }

    @Override
    public InputStream getEntityStream() {
        return state.getResponseEntityStream();
    }

    @Override
    public void setEntityStream(InputStream input) {
        state.setResponseEntityStream(input);
    }
}
