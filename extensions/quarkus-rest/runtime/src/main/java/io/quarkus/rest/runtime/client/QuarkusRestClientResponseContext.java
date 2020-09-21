package io.quarkus.rest.runtime.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.StatusType;

import io.quarkus.rest.runtime.jaxrs.QuarkusRestStatusType;
import io.quarkus.rest.runtime.util.DateUtil;
import io.quarkus.rest.runtime.util.HttpHeaderNames;

public class QuarkusRestClientResponseContext implements ClientResponseContext {

    private int status;
    private String reasonPhrase;
    private MultivaluedMap<String, String> headers;
    private InputStream input;
    /**
     * FIXME: allow for streaming data
     */
    private byte[] data;

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

    public InputStream getInput() {
        return input;
    }

    public QuarkusRestClientResponseContext setInput(InputStream input) {
        this.input = input;
        return this;
    }

    public byte[] getData() {
        return data;
    }

    public QuarkusRestClientResponseContext setData(byte[] data) {
        this.data = data;
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
        return headers.getFirst(name);
    }

    @Override
    public Set<String> getAllowedMethods() {
        String cl = headers.getFirst(HttpHeaderNames.ALLOW);
        if (cl == null) {
            return null;
        }
        return Arrays.stream(cl.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    @Override
    public Date getDate() {
        String cl = headers.getFirst(HttpHeaderNames.DATE);
        if (cl == null) {
            return null;
        }
        return DateUtil.parseDate(cl);
    }

    @Override
    public Locale getLanguage() {
        String cl = headers.getFirst(HttpHeaderNames.CONTENT_LANGUAGE);
        if (cl == null) {
            return null;
        }
        return Locale.forLanguageTag(cl);
    }

    @Override
    public int getLength() {
        String cl = headers.getFirst(HttpHeaderNames.CONTENT_LENGTH);
        if (cl == null) {
            return -1;
        }
        return Integer.parseInt(cl);
    }

    @Override
    public MediaType getMediaType() {
        String cl = headers.getFirst(HttpHeaderNames.CONTENT_TYPE);
        if (cl == null) {
            return null;
        }
        return MediaType.valueOf(cl);
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        throw new RuntimeException("NYI");
    }

    @Override
    public EntityTag getEntityTag() {
        String cl = headers.getFirst(HttpHeaderNames.ETAG);
        if (cl == null) {
            return null;
        }
        return EntityTag.valueOf(cl);
    }

    @Override
    public Date getLastModified() {
        String cl = headers.getFirst(HttpHeaderNames.LAST_MODIFIED);
        if (cl == null) {
            return null;
        }
        return DateUtil.parseDate(cl);
    }

    @Override
    public URI getLocation() {
        String cl = headers.getFirst(HttpHeaderNames.LOCATION);
        if (cl == null) {
            return null;
        }
        try {
            return new URI(cl);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public Set<Link> getLinks() {
        return Collections.emptySet();
    }

    @Override
    public boolean hasLink(String relation) {
        return false;
    }

    @Override
    public Link getLink(String relation) {
        return null;
    }

    @Override
    public Builder getLinkBuilder(String relation) {
        return null;
    }

    @Override
    public boolean hasEntity() {
        return data != null;
    }

    @Override
    public InputStream getEntityStream() {
        if (input != null) {
            return input;
        }
        if (data == null) {
            return new ByteArrayInputStream(new byte[0]);
        }
        return input = new ByteArrayInputStream(data);
    }

    @Override
    public void setEntityStream(InputStream input) {
        this.input = input;
    }
}
