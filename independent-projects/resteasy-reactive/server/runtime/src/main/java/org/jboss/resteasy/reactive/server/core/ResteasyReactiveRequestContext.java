package org.jboss.resteasy.reactive.server.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import org.jboss.resteasy.reactive.common.core.AbstractResteasyReactiveContext;
import org.jboss.resteasy.reactive.common.util.Encode;
import org.jboss.resteasy.reactive.common.util.PathSegmentImpl;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.core.serialization.EntityWriter;
import org.jboss.resteasy.reactive.server.injection.ResteasyReactiveInjectionContext;
import org.jboss.resteasy.reactive.server.jaxrs.AsyncResponseImpl;
import org.jboss.resteasy.reactive.server.jaxrs.ContainerRequestContextImpl;
import org.jboss.resteasy.reactive.server.jaxrs.ContainerResponseContextImpl;
import org.jboss.resteasy.reactive.server.jaxrs.HttpHeadersImpl;
import org.jboss.resteasy.reactive.server.jaxrs.ProvidersImpl;
import org.jboss.resteasy.reactive.server.jaxrs.RequestImpl;
import org.jboss.resteasy.reactive.server.jaxrs.SseEventSinkImpl;
import org.jboss.resteasy.reactive.server.jaxrs.UriInfoImpl;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public abstract class ResteasyReactiveRequestContext
        extends AbstractResteasyReactiveContext<ResteasyReactiveRequestContext, ServerRestHandler>
        implements Closeable, ResteasyReactiveInjectionContext, ServerRequestContext {

    public static final Object[] EMPTY_ARRAY = new Object[0];
    protected final Deployment deployment;
    protected final ProvidersImpl providers;
    /**
     * The parameters array, populated by handlers
     */
    private Object[] parameters;
    private RuntimeResource target;

    /**
     * info about path params and other data about previously matched sub resource locators
     */
    private PreviousResource previousResource;

    /**
     * The parameter values extracted from the path.
     * <p>
     * This is not a map, for two reasons. One is raw performance, as an array causes
     * less allocations and is generally faster. The other is that it is possible
     * that you can have equivalent templates with different names. This allows the
     * mapper to ignore the names, as everything is resolved in terms of indexes.
     * <p>
     * If there is only a single path param then it is stored directly into the field,
     * while multiple params this will be an array. This optimisation allows us to avoid
     * allocating anything in the common case that there is zero or one path param.
     * <p>
     * Note: those are decoded.
     */
    private Object pathParamValues;

    private UriInfo uriInfo;
    /**
     * The endpoint to invoke
     */
    private Object endpointInstance;
    /**
     * The result of the invocation
     */
    private Object result;
    /**
     * The supplier of the actual response
     */
    private LazyResponse response;

    private HttpHeadersImpl httpHeaders;
    private Object requestEntity;
    private Request request;
    private EntityWriter entityWriter;
    private ContainerRequestContextImpl containerRequestContext;
    private ContainerResponseContextImpl containerResponseContext;
    private String method; // used to hold the explicitly set method performed by a ContainerRequestFilter
    private String originalMethod; // store the original method as obtaining it from Vert.x isn't dirt cheap
    // this is only set if we override the requestUri
    private String path;
    // this is cached, but only if we override the requestUri
    private String absoluteUri;
    // this is only set if we override the requestUri
    private String scheme;
    // this is only set if we override the requestUri
    private String authority;
    private String remaining;
    private EncodedMediaType responseContentType;
    private MediaType consumesMediaType;

    private Annotation[] methodAnnotations;
    private Annotation[] additionalAnnotations; // can be added by entity annotations or response filters
    private Annotation[] allAnnotations;
    private Type genericReturnType;

    /**
     * The input stream, if an entity is present.
     */
    private InputStream inputStream;

    /**
     * used for {@link UriInfo#getMatchedURIs()}
     */
    private List<UriMatch> matchedURIs;

    private AsyncResponseImpl asyncResponse;
    private SseEventSinkImpl sseEventSink;
    private List<PathSegment> pathSegments;
    private ReaderInterceptor[] readerInterceptors;
    private WriterInterceptor[] writerInterceptors;

    private SecurityContext securityContext;
    private OutputStream outputStream;
    private OutputStream underlyingOutputStream;
    private FormData formData;

    public ResteasyReactiveRequestContext(Deployment deployment, ProvidersImpl providers,
            ThreadSetupAction requestContext, ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain) {
        super(handlerChain, abortHandlerChain, requestContext);
        this.deployment = deployment;
        this.providers = providers;
        this.parameters = EMPTY_ARRAY;
    }

    public abstract ServerHttpRequest serverRequest();

    public abstract ServerHttpResponse serverResponse();

    public Deployment getDeployment() {
        return deployment;
    }

    public ProvidersImpl getProviders() {
        return providers;
    }

    /**
     * Restarts handler chain processing with a new chain targeting a new resource.
     *
     * @param target The resource target
     */
    public void restart(RuntimeResource target) {
        restart(target, false);
    }

    public void restart(RuntimeResource target, boolean setLocatorTarget) {
        this.handlers = target.getHandlerChain();
        position = 0;
        parameters = new Object[target.getParameterTypes().length];
        if (setLocatorTarget) {
            previousResource = new PreviousResource(this.target, pathParamValues, previousResource);
        }
        this.target = target;
    }

    /**
     * Meant to be used when an error occurred early in processing chain
     */
    @Override
    public void abortWith(Response response) {
        setResult(response);
        setAbortHandlerChainStarted(true);
        restart(getAbortHandlerChain());
        // this is a valid action after suspend, in which case we must resume
        if (isSuspended()) {
            resume();
        }
    }

    /**
     * Resets the build time serialization assumptions. Called if a filter
     * modifies the response
     */
    public void resetBuildTimeSerialization() {
        entityWriter = deployment.getDynamicEntityWriter();
    }

    public UriInfo getUriInfo() {
        if (uriInfo == null) {
            uriInfo = new UriInfoImpl(this);
        }
        return uriInfo;
    }

    public HttpHeadersImpl getHttpHeaders() {
        if (httpHeaders == null) {
            httpHeaders = new HttpHeadersImpl(serverRequest().getAllRequestHeaders());
        }
        return httpHeaders;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setMaxPathParams(int maxPathParams) {
        if (maxPathParams > 1) {
            pathParamValues = new String[maxPathParams];
        } else {
            pathParamValues = null;
        }
    }

    public String getPathParam(int index) {
        return doGetPathParam(index, pathParamValues);
    }

    private String doGetPathParam(int index, Object pathParamValues) {
        if (pathParamValues instanceof String[]) {
            return ((String[]) pathParamValues)[index];
        }
        if (index > 1) {
            throw new IndexOutOfBoundsException();
        }
        return (String) pathParamValues;
    }

    public ResteasyReactiveRequestContext setPathParamValue(int index, String value) {
        if (pathParamValues instanceof String[]) {
            ((String[]) pathParamValues)[index] = value;
        } else {
            if (index > 1) {
                throw new IndexOutOfBoundsException();
            }
            pathParamValues = value;
        }
        return this;
    }

    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public Object getRequestEntity() {
        return requestEntity;
    }

    public ResteasyReactiveRequestContext setRequestEntity(Object requestEntity) {
        this.requestEntity = requestEntity;
        return this;
    }

    public EntityWriter getEntityWriter() {
        return entityWriter;
    }

    public ResteasyReactiveRequestContext setEntityWriter(EntityWriter entityWriter) {
        this.entityWriter = entityWriter;
        return this;
    }

    public Object getEndpointInstance() {
        return endpointInstance;
    }

    public ResteasyReactiveRequestContext setEndpointInstance(Object endpointInstance) {
        this.endpointInstance = endpointInstance;
        return this;
    }

    public Object getResult() {
        return result;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Object getResponseEntity() {
        Object result = responseEntity();
        if (result instanceof GenericEntity) {
            return ((GenericEntity<?>) result).getEntity();
        }
        return result;
    }

    private Object responseEntity() {
        if (response != null && response.isCreated()) {
            return response.get().getEntity();
        }
        return result;
    }

    public ResteasyReactiveRequestContext setResult(Object result) {
        this.result = result;
        if (result instanceof Response) {
            this.response = new LazyResponse.Existing((Response) result);
        } else if (result instanceof GenericEntity) {
            setGenericReturnType(((GenericEntity<?>) result).getType());
        }
        return this;
    }

    public boolean handlesUnmappedException() {
        return true;
    }

    public void handleUnmappedException(Throwable throwable) {
        setResult(Response.serverError().build());
    }

    public RuntimeResource getTarget() {
        return target;
    }

    public void mapExceptionIfPresent() {
        // this is called from the abort chain, but we can abort because we have a Response, or because
        // we got an exception
        if (throwable != null) {
            this.responseContentType = null;
            deployment.getExceptionMapping().mapException(throwable, this);
            // NOTE: keep the throwable around for close() AsyncResponse notification
        }
    }

    private void sendInternalError(Throwable throwable) {
        log.error("Request failed", throwable);
        serverResponse().setStatusCode(500).end();
        close();
    }

    @Override
    public void close() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            log.debug("Failed to close stream", e);
        }
        try {
            if (underlyingOutputStream != null) {
                underlyingOutputStream.close();
            }
        } catch (IOException e) {
            log.debug("Failed to close stream", e);
        }
        super.close();
    }

    public LazyResponse getResponse() {
        return response;
    }

    public ResteasyReactiveRequestContext setResponse(LazyResponse response) {
        this.response = response;
        return this;
    }

    public Request getRequest() {
        if (request == null) {
            request = new RequestImpl(this);
        }
        return request;
    }

    public ContainerRequestContextImpl getContainerRequestContext() {
        if (containerRequestContext == null) {
            containerRequestContext = new ContainerRequestContextImpl(this);
        }
        return containerRequestContext;
    }

    public ContainerResponseContextImpl getContainerResponseContext() {
        if (containerResponseContext == null) {
            containerResponseContext = new ContainerResponseContextImpl(this);
        }
        return containerResponseContext;
    }

    public String getMethod() {
        if (method == null) {
            if (originalMethod != null) {
                return originalMethod;
            }
            return originalMethod = serverRequest().getRequestMethod();
        }
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setRemaining(String remaining) {
        this.remaining = remaining;
    }

    public String getRemaining() {
        return remaining;
    }

    /**
     * Returns the normalised non-decoded path excluding any prefix.
     */
    public String getPathWithoutPrefix() {
        String path = getPath();
        if (path != null) {
            String prefix = deployment.getPrefix();
            if (!prefix.isEmpty()) {
                // FIXME: can we really have paths that don't start with the prefix if there's a prefix?
                if (path.startsWith(prefix)) {
                    return path.substring(prefix.length());
                }
            }
        }
        return path;
    }

    /**
     * Returns the normalised non-decoded path including any prefix.
     */
    public String getPath() {
        if (path == null) {
            return serverRequest().getRequestNormalisedPath();
        }
        return path;
    }

    public String getAbsoluteURI() {
        // if we never changed the path we can use the vert.x URI
        if (path == null)
            return serverRequest().getRequestAbsoluteUri();
        // Note: we could store our cache as normalised, but I'm not sure if the vertx one is normalised
        if (absoluteUri == null) {
            try {
                absoluteUri = new URI(scheme, authority, path, null, null).toASCIIString();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return absoluteUri;
    }

    public String getScheme() {
        if (scheme == null)
            return serverRequest().getRequestScheme();
        return scheme;
    }

    public String getAuthority() {
        if (authority == null)
            return serverRequest().getRequestHost();
        return authority;
    }

    public ResteasyReactiveRequestContext setRequestUri(URI requestURI) {
        this.path = requestURI.getPath();
        this.authority = requestURI.getRawAuthority();
        this.scheme = requestURI.getScheme();
        // FIXME: it's possible we may have to also update the query part
        // invalidate those
        this.uriInfo = null;
        this.absoluteUri = null;
        return this;
    }

    /**
     * Returns the current response content type. If a response has been set and has an
     * explicit content type then this is used, otherwise it returns any content type
     * that has been explicitly set.
     */
    @Override
    public EncodedMediaType getResponseContentType() {
        if (response != null) {
            if (response.isCreated()) {
                MediaType mediaType = response.get().getMediaType();
                if (mediaType != null) {
                    return new EncodedMediaType(mediaType);
                }
            }
        }
        return responseContentType;
    }

    @Override
    public MediaType getResponseMediaType() {
        EncodedMediaType resp = getResponseContentType();
        if (resp == null) {
            return null;
        }
        return resp.mediaType;
    }

    public ResteasyReactiveRequestContext setResponseContentType(EncodedMediaType responseContentType) {
        this.responseContentType = responseContentType;
        return this;
    }

    public ResteasyReactiveRequestContext setResponseContentType(MediaType responseContentType) {
        if (responseContentType == null) {
            this.responseContentType = null;
        } else {
            this.responseContentType = new EncodedMediaType(responseContentType);
        }
        return this;
    }

    public MediaType getConsumesMediaType() {
        return consumesMediaType;
    }

    public ResteasyReactiveRequestContext setConsumesMediaType(MediaType consumesMediaType) {
        this.consumesMediaType = consumesMediaType;
        return this;
    }

    public Annotation[] getAllAnnotations() {
        if (allAnnotations == null) {
            Annotation[] methodAnnotations = getMethodAnnotations();
            if ((additionalAnnotations == null) || (additionalAnnotations.length == 0)) {
                allAnnotations = methodAnnotations;
            } else {
                List<Annotation> list = new ArrayList<>(methodAnnotations.length + additionalAnnotations.length);
                list.addAll(Arrays.asList(methodAnnotations));
                list.addAll(Arrays.asList(additionalAnnotations));
                allAnnotations = list.toArray(new Annotation[0]);
            }
        }
        return allAnnotations;
    }

    public void setAllAnnotations(Annotation[] annotations) {
        this.allAnnotations = annotations;
    }

    public Annotation[] getMethodAnnotations() {
        if (methodAnnotations == null) {
            if (target == null) {
                return null;
            }
            return target.getLazyMethod().getAnnotations();
        }
        return methodAnnotations;
    }

    public ResteasyReactiveRequestContext setMethodAnnotations(Annotation[] methodAnnotations) {
        this.methodAnnotations = methodAnnotations;
        return this;
    }

    public Annotation[] getAdditionalAnnotations() {
        return additionalAnnotations;
    }

    public void setAdditionalAnnotations(Annotation[] additionalAnnotations) {
        this.additionalAnnotations = additionalAnnotations;
    }

    public boolean hasGenericReturnType() {
        return this.genericReturnType != null;
    }

    public Type getGenericReturnType() {
        if (genericReturnType == null) {
            if (target == null) {
                return null;
            }
            return target.getLazyMethod().getGenericReturnType();
        }
        return genericReturnType;
    }

    public ResteasyReactiveRequestContext setGenericReturnType(Type genericReturnType) {
        this.genericReturnType = genericReturnType;
        return this;
    }

    public AsyncResponseImpl getAsyncResponse() {
        return asyncResponse;
    }

    public ResteasyReactiveRequestContext setAsyncResponse(AsyncResponseImpl asyncResponse) {
        if (this.asyncResponse != null) {
            throw new RuntimeException("Async can only be started once");
        }
        this.asyncResponse = asyncResponse;
        return this;
    }

    public ReaderInterceptor[] getReaderInterceptors() {
        return readerInterceptors;
    }

    public ResteasyReactiveRequestContext setReaderInterceptors(ReaderInterceptor[] readerInterceptors) {
        this.readerInterceptors = readerInterceptors;
        return this;
    }

    public WriterInterceptor[] getWriterInterceptors() {
        return writerInterceptors;
    }

    public ResteasyReactiveRequestContext setWriterInterceptors(WriterInterceptor[] writerInterceptors) {
        this.writerInterceptors = writerInterceptors;
        return this;
    }

    protected void handleUnrecoverableError(Throwable throwable) {
        log.error("Request failed", throwable);
        if (serverResponse().headWritten()) {
            serverRequest().closeConnection();
        } else {
            serverResponse().setStatusCode(500).end();
        }
        close();
    }

    protected void handleRequestScopeActivation() {
        CurrentRequestManager.set(this);
    }

    @Override
    protected void requestScopeDeactivated() {
        CurrentRequestManager.set(null);
    }

    @Override
    protected void restarted(boolean keepTarget) {
        parameters = new Object[0];
        if (!keepTarget) {
            target = null;
        }
    }

    public void saveUriMatchState() {
        if (matchedURIs == null) {
            matchedURIs = new LinkedList<>();
        } else if (matchedURIs.get(0).resource == target) {
            //already saved
            return;
        }
        if (target != null) {
            URITemplate classPath = target.getClassPath();
            if (classPath != null) {
                //this is not great, but the alternative is to do path based matching on every request
                //given that this method is likely to be called very infrequently it is better to have a small
                //cost here than a cost applied to every request
                int pos = classPath.stem.length();
                String path = getPathWithoutPrefix();
                //we already know that this template matches, we just need to find the matched bit
                for (int i = 1; i < classPath.components.length; ++i) {
                    URITemplate.TemplateComponent segment = classPath.components[i];
                    if (segment.type == URITemplate.Type.LITERAL) {
                        pos += segment.literalText.length();
                    } else if (segment.type == URITemplate.Type.DEFAULT_REGEX) {
                        for (; pos < path.length(); ++pos) {
                            if (path.charAt(pos) == '/') {
                                --pos;
                                break;
                            }
                        }
                    } else {
                        Matcher matcher = segment.pattern.matcher(path);
                        if (matcher.find(pos) && matcher.start() == pos) {
                            pos = matcher.end();
                        }
                    }
                }
                matchedURIs.add(new UriMatch(path.substring(1, pos), null, null));
            }
        }
        // FIXME: this may be better as context.normalisedPath() or getPath()
        // TODO: does this entry make sense when target is null ?
        String path = serverRequest().getRequestPath();
        if (path.equals(remaining)) {
            matchedURIs.add(0, new UriMatch(path.substring(1), target, endpointInstance));
        } else {
            matchedURIs.add(0, new UriMatch(path.substring(1, path.length() - (remaining == null ? 0 : remaining.length())),
                    target, endpointInstance));
        }

    }

    public List<UriMatch> getMatchedURIs() {
        saveUriMatchState();
        return matchedURIs;
    }

    public boolean hasInputStream() {
        return inputStream != null;
    }

    public InputStream getInputStream() {
        if (inputStream == null) {
            inputStream = serverRequest().createInputStream();
        }
        return inputStream;
    }

    public ResteasyReactiveRequestContext setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public SseEventSinkImpl getSseEventSink() {
        return sseEventSink;
    }

    public void setSseEventSink(SseEventSinkImpl sseEventSink) {
        this.sseEventSink = sseEventSink;
    }

    /**
     * Return the path segments
     * <p>
     * This is lazily initialized
     */
    public List<PathSegment> getPathSegments() {
        if (pathSegments == null) {
            initPathSegments();
        }
        return pathSegments;
    }

    /**
     * initializes the path seegments and removes any matrix params for the path
     * used for matching.
     */
    public void initPathSegments() {
        if (pathSegments != null) {
            return;
        }
        //this is not super optimised
        //I don't think we care about it that much though
        String path = getPath();
        String[] parts = path.split("/");
        pathSegments = new ArrayList<>();
        boolean hasMatrix = false;
        for (String i : parts) {
            if (i.isEmpty()) {
                continue;
            }
            PathSegmentImpl ps = new PathSegmentImpl(i, true);
            hasMatrix = ps.hasMatrixParams() || hasMatrix;
            pathSegments.add(ps);
        }
        if (hasMatrix) {
            StringBuilder sb = new StringBuilder();
            for (PathSegment i : pathSegments) {
                sb.append("/");
                sb.append(i.getPath());
            }
            if (path.endsWith("/")) {
                sb.append("/");
            }
            String newPath = sb.toString();
            this.path = newPath;
            if (this.remaining != null) {
                this.remaining = newPath.substring(getPathWithoutPrefix().length() - this.remaining.length());
            }
        }
    }

    @Override
    public Object getHeader(String name, boolean single) {
        if (httpHeaders == null) {
            if (single)
                return serverRequest().getRequestHeader(name);
            // empty collections must not be turned to null
            return serverRequest().getAllRequestHeaders(name);
        } else {
            if (single)
                return httpHeaders.getMutableHeaders().getFirst(name);
            // empty collections must not be turned to null
            List<String> list = httpHeaders.getMutableHeaders().get(name);
            if (list == null) {
                return Collections.emptyList();
            } else {
                return list;
            }
        }
    }

    @Override
    public Object getQueryParameter(String name, boolean single, boolean encoded) {
        if (single) {
            String val = serverRequest().getQueryParam(name);
            if (encoded && val != null) {
                val = Encode.encodeQueryParam(val);
            }
            return val;
        }
        // empty collections must not be turned to null
        List<String> strings = serverRequest().getAllQueryParams(name);
        if (encoded) {
            List<String> newStrings = new ArrayList<>();
            for (String i : strings) {
                newStrings.add(Encode.encodeQueryParam(i));
            }
            return newStrings;
        }
        return strings;
    }

    @Override
    public Object getMatrixParameter(String name, boolean single, boolean encoded) {
        if (single) {
            for (PathSegment i : getPathSegments()) {
                String res = i.getMatrixParameters().getFirst(name);
                if (res != null) {
                    if (encoded) {
                        return Encode.encodeQueryParam(res);
                    }
                    return res;
                }
            }
            return null;
        } else {
            List<String> ret = new ArrayList<>();
            for (PathSegment i : getPathSegments()) {
                List<String> res = i.getMatrixParameters().get(name);
                if (res != null) {
                    if (encoded) {
                        for (String j : res) {
                            ret.add(Encode.encodeQueryParam(j));
                        }
                    } else {
                        ret.addAll(res);
                    }
                }
            }
            // empty collections must not be turned to null
            return ret;
        }
    }

    @Override
    public String getCookieParameter(String name) {
        Cookie cookie = getHttpHeaders().getCookies().get(name);
        return cookie != null ? cookie.getValue() : null;
    }

    @Override
    public Object getFormParameter(String name, boolean single, boolean encoded) {
        if (formData == null) {
            return null;
        }
        if (single) {
            FormData.FormValue val = formData.getFirst(name);
            if (val == null || val.isFileItem()) {
                return null;
            }
            if (encoded) {
                return Encode.encodeQueryParam(val.getValue());
            }
            return val.getValue();
        }
        Deque<FormData.FormValue> val = formData.get(name);
        List<String> strings = new ArrayList<>();
        if (val != null) {
            for (FormData.FormValue i : val) {
                if (encoded) {
                    strings.add(Encode.encodeQueryParam(i.getValue()));
                } else {
                    strings.add(i.getValue());
                }
            }
        }
        return strings;

    }

    @Override
    public String getPathParameter(String name, boolean encoded) {
        // this is a slower version than getPathParam, but we can't actually bake path indices inside
        // BeanParam classes (which use thismethod ) because they can be used by multiple resources that would have different
        // indices
        Integer index = this.target.getPathParameterIndexes().get(name);
        // It's possible to inject a path param that's not defined, return null in this case
        String value = index != null ? getPathParam(index) : null;
        if (encoded && value != null) {
            return Encode.encodeQueryParam(value);
        }
        return value;
    }

    public <T> T unwrap(Class<T> type) {
        return serverRequest().unwrap(type);
    }

    public SecurityContext getSecurityContext() {
        if (securityContext == null) {
            securityContext = createSecurityContext();
        }
        return securityContext;
    }

    public boolean isSecurityContextSet() {
        return securityContext != null;
    }

    protected SecurityContext createSecurityContext() {
        throw new UnsupportedOperationException();
    }

    public ResteasyReactiveRequestContext setSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
        securityContextUpdated(securityContext);
        return this;
    }

    protected void securityContextUpdated(SecurityContext securityContext) {

    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public OutputStream getOrCreateOutputStream() {
        if (outputStream == null) {
            return outputStream = underlyingOutputStream = serverResponse().createResponseOutputStream();
        }
        return outputStream;
    }

    @Override
    public ResteasyReactiveResourceInfo getResteasyReactiveResourceInfo() {
        return target == null ? null : target.getLazyMethod();
    }

    @Override
    protected abstract Executor getEventLoop();

    public abstract Runnable registerTimer(long millis, Runnable task);

    public String getResourceLocatorPathParam(String name) {
        return getResourceLocatorPathParam(name, previousResource);
    }

    public FormData getFormData() {
        return formData;
    }

    public ResteasyReactiveRequestContext setFormData(FormData formData) {
        this.formData = formData;
        return this;
    }

    private String getResourceLocatorPathParam(String name, PreviousResource previousResource) {
        if (previousResource == null) {
            return null;
        }

        int index = 0;
        URITemplate classPath = previousResource.locatorTarget.getClassPath();
        if (classPath != null) {
            for (URITemplate.TemplateComponent component : classPath.components) {
                if (component.name != null) {
                    if (component.name.equals(name)) {
                        return doGetPathParam(index, previousResource.locatorPathParamValues);
                    }
                    index++;
                } else if (component.names != null) {
                    for (String nm : component.names) {
                        if (nm.equals(name)) {
                            return doGetPathParam(index, previousResource.locatorPathParamValues);
                        }
                    }
                    index++;
                }
            }
        }
        for (URITemplate.TemplateComponent component : previousResource.locatorTarget.getPath().components) {
            if (component.name != null) {
                if (component.name.equals(name)) {
                    return doGetPathParam(index, previousResource.locatorPathParamValues);
                }
                index++;
            } else if (component.names != null) {
                for (String nm : component.names) {
                    if (nm.equals(name)) {
                        return doGetPathParam(index, previousResource.locatorPathParamValues);
                    }
                }
                index++;
            }
        }
        return getResourceLocatorPathParam(name, previousResource.prev);
    }

    static class PreviousResource {

        public PreviousResource(RuntimeResource locatorTarget, Object locatorPathParamValues, PreviousResource prev) {
            this.locatorTarget = locatorTarget;
            this.locatorPathParamValues = locatorPathParamValues;
            this.prev = prev;
        }

        /**
         * When a subresource has been located and the processing has been restarted (and thus target point to the new
         * subresource),
         * this field contains the target that resulted in the offloading to the new target
         */
        private final RuntimeResource locatorTarget;

        /**
         * When a subresource has been located and the processing has been restarted (and thus target point to the new
         * subresource),
         * this field contains the pathParamValues of the target that resulted in the offloading to the new target
         */
        private final Object locatorPathParamValues;

        private final PreviousResource prev;

    }
}
