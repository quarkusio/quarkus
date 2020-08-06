package io.quarkus.qrs.runtime.jaxrs;

import static io.quarkus.qrs.runtime.util.HttpHeaderNames.ACCEPT;
import static io.quarkus.qrs.runtime.util.HttpHeaderNames.ACCEPT_CHARSET;
import static io.quarkus.qrs.runtime.util.HttpHeaderNames.ACCEPT_ENCODING;
import static io.quarkus.qrs.runtime.util.HttpHeaderNames.ACCEPT_LANGUAGE;
import static io.quarkus.qrs.runtime.util.HttpHeaderNames.IF_MATCH;
import static io.quarkus.qrs.runtime.util.HttpHeaderNames.IF_MODIFIED_SINCE;
import static io.quarkus.qrs.runtime.util.HttpHeaderNames.IF_NONE_MATCH;
import static io.quarkus.qrs.runtime.util.HttpHeaderNames.IF_UNMODIFIED_SINCE;
import static io.quarkus.qrs.runtime.util.HttpHeaderNames.VARY;
import static io.quarkus.qrs.runtime.util.HttpResponseCodes.SC_PRECONDITION_FAILED;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.request.ServerDrivenNegotiation;
import io.quarkus.qrs.runtime.util.DateUtil;

public class QrsRequest implements Request {

    private final QrsRequestContext requestContext;
    private final String httpMethod;
    private String varyHeader;

    public QrsRequest(QrsRequestContext requestContext) {
        this.requestContext = requestContext;
        this.httpMethod = requestContext.getContext().request().method().name();
    }

    @Override
    public String getMethod() {
        return requestContext.getContext().request().method().name();
    }

    private boolean isRfc7232preconditions() {
        return true;//todo: do we need config for this?
    }

    public Variant selectVariant(List<Variant> variants) throws IllegalArgumentException {
        if (variants == null || variants.size() == 0)
            throw new IllegalArgumentException("Variant list must not be empty");

        ServerDrivenNegotiation negotiation = new ServerDrivenNegotiation();
        MultivaluedMap<String, String> requestHeaders = requestContext.getHttpHeaders().getRequestHeaders();
        negotiation.setAcceptHeaders(requestHeaders.get(ACCEPT));
        negotiation.setAcceptCharsetHeaders(requestHeaders.get(ACCEPT_CHARSET));
        negotiation.setAcceptEncodingHeaders(requestHeaders.get(ACCEPT_ENCODING));
        negotiation.setAcceptLanguageHeaders(requestHeaders.get(ACCEPT_LANGUAGE));

        varyHeader = QrsResponseBuilder.createVaryHeader(variants);
        requestContext.getContext().response().headers().set(VARY, varyHeader);
        //response.getOutputHeaders().add(VARY, varyHeader);
        return negotiation.getBestMatch(variants);
    }

    public List<EntityTag> convertEtag(List<String> tags) {
        ArrayList<EntityTag> result = new ArrayList<EntityTag>();
        for (String tag : tags) {
            String[] split = tag.split(",");
            for (String etag : split) {
                result.add(EntityTag.valueOf(etag.trim()));
            }
        }
        return result;
    }

    public Response.ResponseBuilder ifMatch(List<EntityTag> ifMatch, EntityTag eTag) {
        boolean match = false;
        for (EntityTag tag : ifMatch) {
            if (tag.equals(eTag) || tag.getValue().equals("*")) {
                match = true;
                break;
            }
        }
        if (match)
            return null;
        return Response.status(SC_PRECONDITION_FAILED).tag(eTag);

    }

    public Response.ResponseBuilder ifNoneMatch(List<EntityTag> ifMatch, EntityTag eTag) {
        boolean match = false;
        for (EntityTag tag : ifMatch) {
            if (tag.equals(eTag) || tag.getValue().equals("*")) {
                match = true;
                break;
            }
        }
        if (match) {
            if ("GET".equals(httpMethod) || "HEAD".equals(httpMethod)) {
                return Response.notModified(eTag);
            }

            return Response.status(SC_PRECONDITION_FAILED).tag(eTag);
        }
        return null;
    }

    public Response.ResponseBuilder evaluatePreconditions(EntityTag eTag) {
        if (eTag == null)
            throw new IllegalArgumentException("ETag was null");
        Response.ResponseBuilder builder = null;
        List<String> ifMatch = requestContext.getHttpHeaders().getRequestHeaders().get(IF_MATCH);
        if (ifMatch != null && ifMatch.size() > 0) {
            builder = ifMatch(convertEtag(ifMatch), eTag);
        }
        if (builder == null) {
            List<String> ifNoneMatch = requestContext.getHttpHeaders().getRequestHeaders().get(IF_NONE_MATCH);
            if (ifNoneMatch != null && ifNoneMatch.size() > 0) {
                builder = ifNoneMatch(convertEtag(ifNoneMatch), eTag);
            }
        }
        if (builder != null) {
            builder.tag(eTag);
        }
        if (builder != null && varyHeader != null)
            builder.header(VARY, varyHeader);
        return builder;
    }

    public Response.ResponseBuilder ifModifiedSince(String strDate, Date lastModified) {
        Date date = DateUtil.parseDate(strDate);

        if (date.getTime() >= lastModified.getTime()) {
            return Response.notModified();
        }
        return null;

    }

    public Response.ResponseBuilder ifUnmodifiedSince(String strDate, Date lastModified) {
        Date date = DateUtil.parseDate(strDate);

        if (date.getTime() >= lastModified.getTime()) {
            return null;
        }
        return Response.status(SC_PRECONDITION_FAILED).lastModified(lastModified);

    }

    public Response.ResponseBuilder evaluatePreconditions(Date lastModified) {
        if (lastModified == null)
            throw new IllegalArgumentException("Param cannot be null");
        Response.ResponseBuilder builder = null;
        MultivaluedMap<String, String> headers = requestContext.getHttpHeaders().getRequestHeaders();
        String ifModifiedSince = headers.getFirst(IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && (!isRfc7232preconditions() || (!headers.containsKey(IF_NONE_MATCH)))) {
            builder = ifModifiedSince(ifModifiedSince, lastModified);
        }
        if (builder == null) {
            String ifUnmodifiedSince = headers.getFirst(IF_UNMODIFIED_SINCE);
            if (ifUnmodifiedSince != null && (!isRfc7232preconditions() || (!headers.containsKey(IF_MATCH)))) {
                builder = ifUnmodifiedSince(ifUnmodifiedSince, lastModified);
            }
        }
        if (builder != null && varyHeader != null)
            builder.header(VARY, varyHeader);

        return builder;
    }

    public Response.ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
        if (lastModified == null)
            throw new IllegalArgumentException("Last modified was null");
        if (eTag == null)
            throw new IllegalArgumentException("etag was null");
        Response.ResponseBuilder rtn = null;
        Response.ResponseBuilder lastModifiedBuilder = evaluatePreconditions(lastModified);
        Response.ResponseBuilder etagBuilder = evaluatePreconditions(eTag);
        if (lastModifiedBuilder == null && etagBuilder == null)
            rtn = null;
        else if (lastModifiedBuilder != null && etagBuilder == null)
            rtn = lastModifiedBuilder;
        else if (lastModifiedBuilder == null && etagBuilder != null)
            rtn = etagBuilder;
        else {
            rtn = lastModifiedBuilder;
            rtn.tag(eTag);
        }
        if (rtn != null && varyHeader != null)
            rtn.header(VARY, varyHeader);
        return rtn;
    }

    public Response.ResponseBuilder evaluatePreconditions() {
        List<String> ifMatch = requestContext.getHttpHeaders().getRequestHeaders().get(IF_MATCH);
        if (ifMatch == null || ifMatch.size() == 0) {
            return null;
        }

        return Response.status(SC_PRECONDITION_FAILED);
    }

}
