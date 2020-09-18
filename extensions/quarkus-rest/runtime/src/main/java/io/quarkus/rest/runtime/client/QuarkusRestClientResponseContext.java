package io.quarkus.rest.runtime.client;

import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.StatusType;

import io.quarkus.rest.runtime.NotImplementedYet;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponse;

public class QuarkusRestClientResponseContext implements ClientResponseContext {

    private QuarkusRestResponse response;

    QuarkusRestClientResponseContext(QuarkusRestResponse response) {
        this.response = response;
    }

    private QuarkusRestResponse getResponse() {
        return response;
    }

    @Override
    public int getStatus() {
        return getResponse().getStatus();
    }

    @Override
    public void setStatus(int code) {
        getResponse().setStatus(code);
    }

    @Override
    public StatusType getStatusInfo() {
        return getResponse().getStatusInfo();
    }

    @Override
    public void setStatusInfo(StatusType statusInfo) {
        getResponse().setStatusInfo(statusInfo);
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return getResponse().getStringHeaders();
    }

    @Override
    public String getHeaderString(String name) {
        return getResponse().getHeaderString(name);
    }

    @Override
    public Set<String> getAllowedMethods() {
        return getResponse().getAllowedMethods();
    }

    @Override
    public Date getDate() {
        return getResponse().getDate();
    }

    @Override
    public Locale getLanguage() {
        return getResponse().getLanguage();
    }

    @Override
    public int getLength() {
        return getResponse().getLength();
    }

    @Override
    public MediaType getMediaType() {
        return getResponse().getMediaType();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return getResponse().getCookies();
    }

    @Override
    public EntityTag getEntityTag() {
        return getResponse().getEntityTag();
    }

    @Override
    public Date getLastModified() {
        return getResponse().getLastModified();
    }

    @Override
    public URI getLocation() {
        return getResponse().getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return getResponse().getLinks();
    }

    @Override
    public boolean hasLink(String relation) {
        return getResponse().hasLink(relation);
    }

    @Override
    public Link getLink(String relation) {
        return getResponse().getLink(relation);
    }

    @Override
    public Builder getLinkBuilder(String relation) {
        return getResponse().getLinkBuilder(relation);
    }

    @Override
    public boolean hasEntity() {
        return getResponse().hasEntity();
    }

    @Override
    public InputStream getEntityStream() {
        throw new NotImplementedYet();
    }

    @Override
    public void setEntityStream(InputStream input) {
        throw new NotImplementedYet();
    }
}
