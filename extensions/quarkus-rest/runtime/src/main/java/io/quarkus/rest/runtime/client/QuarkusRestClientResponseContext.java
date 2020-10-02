package io.quarkus.rest.runtime.client;

import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.HashSet;
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

import io.quarkus.rest.runtime.headers.HeaderUtil;
import io.quarkus.rest.runtime.headers.LinkHeaders;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestStatusType;

public class QuarkusRestClientResponseContext implements ClientResponseContext {

    private int status;
    private String reasonPhrase;
    private MultivaluedMap<String, String> headers;
    private InputStream input;

    public QuarkusRestClientResponseContext(int status, String reasonPhrase, MultivaluedMap<String, String> headers) {
        this.status = status;
        this.reasonPhrase = reasonPhrase;
        this.headers = headers;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public QuarkusRestClientResponseContext setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
        return this;
    }

    public QuarkusRestClientResponseContext setHeaders(MultivaluedMap<String, String> headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int code) {
        this.status = code;
    }

    @Override
    public StatusType getStatusInfo() {
        return new QuarkusRestStatusType(status, reasonPhrase);
    }

    @Override
    public void setStatusInfo(StatusType statusInfo) {
        status = statusInfo.getStatusCode();
        reasonPhrase = statusInfo.getReasonPhrase();
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getHeaderString(String name) {
        return HeaderUtil.getHeaderString(headers, name);
    }

    @Override
    public Set<String> getAllowedMethods() {
        return HeaderUtil.getAllowedMethods(headers);
    }

    @Override
    public Date getDate() {
        return HeaderUtil.getDate(headers);
    }

    @Override
    public Locale getLanguage() {
        return HeaderUtil.getLanguage(headers);
    }

    @Override
    public int getLength() {
        return HeaderUtil.getLength(headers);
    }

    @Override
    public MediaType getMediaType() {
        return HeaderUtil.getMediaType(headers);
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return HeaderUtil.getCookies(headers);
    }

    @Override
    public EntityTag getEntityTag() {
        return HeaderUtil.getEntityTag(headers);
    }

    @Override
    public Date getLastModified() {
        return HeaderUtil.getLastModified(headers);
    }

    @Override
    public URI getLocation() {
        return HeaderUtil.getLocation(headers);
    }

    private LinkHeaders getLinkHeaders() {
        return new LinkHeaders((MultivaluedMap) headers);
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
        return input != null;
    }

    @Override
    public InputStream getEntityStream() {
        return input;
    }

    @Override
    public void setEntityStream(InputStream input) {
        this.input = input;
    }
}
