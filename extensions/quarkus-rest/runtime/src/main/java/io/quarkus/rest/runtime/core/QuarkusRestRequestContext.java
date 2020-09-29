package io.quarkus.rest.runtime.core;

import java.io.Closeable;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.jboss.logging.Logger;

import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.quarkus.rest.runtime.core.serialization.EntityWriter;
import io.quarkus.rest.runtime.handlers.RestHandler;
import io.quarkus.rest.runtime.injection.QuarkusRestInjectionContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestAsyncResponse;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestContainerRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestHttpHeaders;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestProviders;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestRequest;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestSecurityContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestSseEventSink;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestUriInfo;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
import io.quarkus.rest.runtime.mapping.URITemplate;
import io.quarkus.rest.runtime.util.EmptyInputStream;
import io.quarkus.rest.runtime.util.PathSegmentImpl;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;

public class QuarkusRestRequestContext implements Runnable, Closeable, QuarkusRestInjectionContext {
    private static final Logger log = Logger.getLogger(QuarkusRestRequestContext.class);
    public static final Object[] EMPTY_ARRAY = new Object[0];
    private final QuarkusRestDeployment deployment;
    private final QuarkusRestProviders providers;
    private final RoutingContext context;
    private final ManagedContext requestContext;
    private final CurrentVertxRequest currentVertxRequest;
    private InjectableContext.ContextState currentRequestScope;
    /**
     * The parameters array, populated by handlers
     */
    private Object[] parameters;
    private RuntimeResource target;
    private RestHandler[] handlers;
    private RestHandler[] abortHandlerChain;

    /**
     * The parameter values extracted from the path.
     *
     * This is not a map, for two reasons. One is raw performance, as an array causes
     * less allocations and is generally faster. The other is that it is possible
     * that you can have equivalent templates with different names. This allows the
     * mapper to ignore the names, as everything is resolved in terms of indexes.
     * 
     * If there is only a single path param then it is stored directly into the field,
     * while multiple params this will be an array. This optimisation allows us to avoid
     * allocating anything in the common case that there is zero or one path param.
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
    private boolean suspended = false;
    private volatile boolean resetRequestContext = false;
    private volatile boolean running = false;
    private volatile Executor executor;
    private int position;
    private Throwable throwable;
    private QuarkusRestHttpHeaders httpHeaders;
    private Object requestEntity;
    private Map<String, Object> properties;
    private Request request;
    private EntityWriter entityWriter;
    private QuarkusRestContainerRequestContext containerRequestContext;
    private String method;
    private String path;
    private String remaining;
    private MediaType producesMediaType;
    private MediaType consumesMediaType;

    private Annotation[] methodAnnotations;
    private Annotation[] additionalAnnotations; // can be added by entity annotations or response filters
    private Annotation[] allAnnotations;
    private Type genericReturnType;

    /**
     * The input stream, if an entity is present.
     */
    private InputStream inputStream = EmptyInputStream.INSTANCE;

    /**
     * used for {@link UriInfo#getMatchedURIs()}
     */
    private List<UriMatch> matchedURIs;

    private QuarkusRestAsyncResponse asyncResponse;
    private QuarkusRestSseEventSink sseEventSink;
    private List<PathSegment> pathSegments;
    private ReaderInterceptor[] readerInterceptors;
    private WriterInterceptor[] writerInterceptors;

    private SecurityContext securityContext;

    public QuarkusRestRequestContext(QuarkusRestDeployment deployment, QuarkusRestProviders providers, RoutingContext context,
            ManagedContext requestContext,
            CurrentVertxRequest currentVertxRequest, RestHandler[] handlerChain, RestHandler[] abortHandlerChain) {
        this.deployment = deployment;
        this.providers = providers;
        this.context = context;
        this.requestContext = requestContext;
        this.currentVertxRequest = currentVertxRequest;
        this.handlers = handlerChain;
        this.abortHandlerChain = abortHandlerChain;
        this.parameters = EMPTY_ARRAY;
    }

    public QuarkusRestDeployment getDeployment() {
        return deployment;
    }

    public QuarkusRestProviders getProviders() {
        return providers;
    }

    public void suspend() {
        suspended = true;
    }

    public void resume() {
        resume(null, null);
    }

    public synchronized void resume(Executor executor) {
        resume(executor, null);
    }

    public synchronized void resume(Throwable throwable) {
        resume(null, throwable);
    }

    public synchronized void resume(Executor executor, Throwable throwable) {
        if (throwable != null) {
            this.throwable = throwable;
        }
        if (running) {
            this.executor = executor;
            if (executor == null) {
                suspended = false;
            }
        } else {
            suspended = false;
            resetRequestContext = true;
            if (executor == null) {
                ((ConnectionBase) context.request().connection()).getContext().nettyEventLoop().execute(this);
            } else {
                executor.execute(this);
            }
        }
    }

    @Override
    public void run() {
        running = true;
        //if this is a blocking target we don't activate for the initial non-blocking part
        //unless there are pre-mapping filters as these may require CDI
        boolean activationRequired = target == null || (target.isBlocking() && executor == null) || resetRequestContext;
        if (activationRequired) {
            if (currentRequestScope == null) {
                requestContext.activate();
                currentVertxRequest.setCurrent(context, this);
            } else {
                requestContext.activate(currentRequestScope);
            }
            resetRequestContext = false;
        }
        try {
            while (position < handlers.length) {
                int pos = position;
                position++; //increment before, as reset may reset it to zero
                try {
                    handlers[pos].handle(this);
                    if (suspended) {
                        Executor exec = null;
                        synchronized (this) {
                            if (this.executor != null) {
                                //resume happened in the meantime
                                suspended = false;
                                exec = this.executor;
                            } else if (suspended) {
                                running = false;
                                return;
                            }
                        }
                        if (exec != null) {
                            //outside sync block
                            exec.execute(this);
                            return;
                        }
                    }
                } catch (Throwable t) {
                    if (handlers == abortHandlerChain) {
                        handleException(t);
                        return;
                    } else {
                        invokeExceptionMapper(t);
                        restart(abortHandlerChain);
                    }
                }
            }
        } catch (Throwable t) {
            handleException(t);
            close();
        } finally {
            running = false;
            if (activationRequired) {
                if (position == handlers.length) {
                    requestContext.terminate();
                    close();
                } else {
                    currentRequestScope = requestContext.getState();
                    requestContext.deactivate();
                }
            }
        }
    }

    /**
     * Restarts handler chain processing on a chain that does not target a specific resource
     *
     * Generally used to abort processing.
     *
     * @param newHandlerChain The new handler chain
     */
    public void restart(RestHandler[] newHandlerChain) {
        this.handlers = newHandlerChain;
        position = 0;
        parameters = new Object[0];
        target = null;
    }

    /**
     * Restarts handler chain processing with a new chain targeting a new resource.
     *
     * @param target The resource target
     */
    public void restart(RuntimeResource target) {
        this.handlers = target.getHandlerChain();
        position = 0;
        parameters = new Object[target.getParameterTypes().length];
        this.target = target;
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
            uriInfo = new QuarkusRestUriInfo(this);
        }
        return uriInfo;
    }

    public QuarkusRestHttpHeaders getHttpHeaders() {
        if (httpHeaders == null) {
            httpHeaders = new QuarkusRestHttpHeaders(context.request().headers());
        }
        return httpHeaders;
    }

    public RoutingContext getContext() {
        return context;
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
        if (pathParamValues instanceof String[]) {
            return ((String[]) pathParamValues)[index];
        }
        if (index > 1) {
            throw new IndexOutOfBoundsException();
        }
        return (String) pathParamValues;
    }

    public QuarkusRestRequestContext setPathParamValue(int index, String value) {
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

    public QuarkusRestRequestContext setRequestEntity(Object requestEntity) {
        this.requestEntity = requestEntity;
        return this;
    }

    public EntityWriter getEntityWriter() {
        return entityWriter;
    }

    public QuarkusRestRequestContext setEntityWriter(EntityWriter entityWriter) {
        this.entityWriter = entityWriter;
        return this;
    }

    public Object getEndpointInstance() {
        return endpointInstance;
    }

    public QuarkusRestRequestContext setEndpointInstance(Object endpointInstance) {
        this.endpointInstance = endpointInstance;
        return this;
    }

    public Object getResult() {
        return result;
    }

    public QuarkusRestRequestContext setResult(Object result) {
        this.result = result;
        return this;
    }

    public RuntimeResource getTarget() {
        return target;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public QuarkusRestRequestContext setSuspended(boolean suspended) {
        this.suspended = suspended;
        return this;
    }

    public boolean isRunning() {
        return running;
    }

    public QuarkusRestRequestContext setRunning(boolean running) {
        this.running = running;
        return this;
    }

    public Executor getExecutor() {
        return executor;
    }

    public QuarkusRestRequestContext setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public int getPosition() {
        return position;
    }

    public QuarkusRestRequestContext setPosition(int position) {
        this.position = position;
        return this;
    }

    public RestHandler[] getHandlers() {
        return handlers;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * ATM this can only be called by the InvocationHandler
     */
    public QuarkusRestRequestContext setThrowable(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }

    private void invokeExceptionMapper(Throwable throwable) {
        this.producesMediaType = null;
        this.result = deployment.getExceptionMapping().mapException(throwable);
    }

    private void handleException(Throwable throwable) {
        log.error("Request failed", throwable);
        context.response().setStatusCode(500).end();
    }

    @Override
    public void close() {
        //TODO: do we even have any resources to close?
        // FIXME: probably move request context termination here, otherwise we can only terminate it from run()
    }

    public Response getResponse() {
        return (Response) result;
    }

    public Object getProperty(String name) {
        if (properties == null) {
            return null;
        }
        return properties.get(name);
    }

    public Collection<String> getPropertyNames() {
        if (properties == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableSet(properties.keySet());
    }

    public void setProperty(String name, Object object) {
        if (object == null) {
            removeProperty(name);
            return;
        }
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(name, object);
    }

    public void removeProperty(String name) {
        if (properties == null) {
            return;
        }
        properties.remove(name);
    }

    public Request getRequest() {
        if (request == null) {
            request = new QuarkusRestRequest(this);
        }
        return request;
    }

    public QuarkusRestContainerRequestContext getContainerRequestContext() {
        if (containerRequestContext == null) {
            containerRequestContext = new QuarkusRestContainerRequestContext(this);
        }
        return containerRequestContext;
    }

    public String getMethod() {
        if (method == null) {
            return context.request().rawMethod();
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

    public String getPathWithoutPrefix() {
        String path = getPath();
        if (path != null) {
            String prefix = deployment.getPrefix();
            if (prefix != null && !prefix.isEmpty() && !prefix.equals("/")) {
                // FIXME: can we really have paths that don't start with the prefix if there's a prefix?
                if (path.startsWith(prefix)) {
                    return path.substring(prefix.length());
                }
            }
        }
        return path;
    }

    public String getPath() {
        if (path == null) {
            return context.normalisedPath();
        }
        return path;
    }

    public QuarkusRestRequestContext setPath(String path) {
        this.path = path;
        return this;
    }

    public MediaType getProducesMediaType() {
        return producesMediaType;
    }

    public QuarkusRestRequestContext setProducesMediaType(MediaType producesMediaType) {
        this.producesMediaType = producesMediaType;
        return this;
    }

    public MediaType getConsumesMediaType() {
        return consumesMediaType;
    }

    public QuarkusRestRequestContext setConsumesMediaType(MediaType consumesMediaType) {
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

    public QuarkusRestRequestContext setMethodAnnotations(Annotation[] methodAnnotations) {
        this.methodAnnotations = methodAnnotations;
        return this;
    }

    public Annotation[] getAdditionalAnnotations() {
        return additionalAnnotations;
    }

    public void setAdditionalAnnotations(Annotation[] additionalAnnotations) {
        this.additionalAnnotations = additionalAnnotations;
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

    public QuarkusRestRequestContext setGenericReturnType(Type genericReturnType) {
        this.genericReturnType = genericReturnType;
        return this;
    }

    public QuarkusRestAsyncResponse getAsyncResponse() {
        return asyncResponse;
    }

    public QuarkusRestRequestContext setAsyncResponse(QuarkusRestAsyncResponse asyncResponse) {
        if (this.asyncResponse != null) {
            throw new RuntimeException("Async can only be started once");
        }
        this.asyncResponse = asyncResponse;
        return this;
    }

    public ReaderInterceptor[] getReaderInterceptors() {
        return readerInterceptors;
    }

    public QuarkusRestRequestContext setReaderInterceptors(ReaderInterceptor[] readerInterceptors) {
        this.readerInterceptors = readerInterceptors;
        return this;
    }

    public WriterInterceptor[] getWriterInterceptors() {
        return writerInterceptors;
    }

    public QuarkusRestRequestContext setWriterInterceptors(WriterInterceptor[] writerInterceptors) {
        this.writerInterceptors = writerInterceptors;
        return this;
    }

    public void saveUriMatchState() {
        if (matchedURIs == null) {
            matchedURIs = new LinkedList<>();
        } else if (matchedURIs.get(0).resource == target) {
            //already saved
            return;
        }
        URITemplate classPath = target.getClassPath();
        if (classPath != null) {
            //this is not great, but the alternative is to do path based matching on every request
            //given that this method is likely to be called very infrequently it is better to have a small
            //cost here than a cost applied to every request
            int pos = classPath.stem.length();
            String path = context.request().path();
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
        String path = context.request().path();
        matchedURIs.add(0, new UriMatch(path.substring(1, path.length() - (remaining == null ? 0 : remaining.length())),
                target, endpointInstance));
    }

    public List<UriMatch> getMatchedURIs() {
        saveUriMatchState();
        return matchedURIs;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public QuarkusRestRequestContext setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public QuarkusRestSseEventSink getSseEventSink() {
        return sseEventSink;
    }

    public void setSseEventSink(QuarkusRestSseEventSink sseEventSink) {
        this.sseEventSink = sseEventSink;
    }

    public RestHandler[] getAbortHandlerChain() {
        return abortHandlerChain;
    }

    public QuarkusRestRequestContext setAbortHandlerChain(RestHandler[] abortHandlerChain) {
        this.abortHandlerChain = abortHandlerChain;
        return this;
    }

    /**
     * Return the path segments
     *
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
        if (single)
            return context.request().getHeader(name);
        // empty collections must not be turned to null
        return getContext().request().headers().getAll(name);
    }

    @Override
    public Object getQueryParameter(String name, boolean single) {
        if (single)
            return context.queryParams().get(name);
        // empty collections must not be turned to null
        return context.queryParam(name);
    }

    @Override
    public Object getMatrixParameter(String name, boolean single) {
        if (single) {
            for (PathSegment i : getPathSegments()) {
                String res = i.getMatrixParameters().getFirst(name);
                if (res != null) {
                    return res;
                }
            }
            return null;
        } else {
            List<String> ret = new ArrayList<>();
            for (PathSegment i : getPathSegments()) {
                List<String> res = i.getMatrixParameters().get(name);
                if (res != null) {
                    ret.addAll(res);
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
    public Object getFormParameter(String name, boolean single) {
        if (single)
            return getContext().request().getFormAttribute(name);
        // empty collections must not be turned to null
        return getContext().request().formAttributes().getAll(name);
    }

    @Override
    public String getPathParameter(String name) {
        // this is a slower version than getPathParam, but we can't actually bake path indices inside
        // BeanParam classes (which use thismethod ) because they can be used by multiple resources that would have different
        // indices
        Integer index = this.target.getPathParameterIndexes().get(name);
        // It's possible to inject a path param that's not defined, return null in this case
        return index != null ? getPathParam(index) : null;
    }

    public SecurityContext getSecurityContext() {
        if (securityContext == null) {
            securityContext = new QuarkusRestSecurityContext(context);
        }
        return securityContext;
    }

    public QuarkusRestRequestContext setSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
        return this;
    }
}
