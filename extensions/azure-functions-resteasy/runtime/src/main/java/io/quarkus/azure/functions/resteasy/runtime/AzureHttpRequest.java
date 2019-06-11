package io.quarkus.azure.functions.resteasy.runtime;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.SynchronousExecutionContext;
import org.jboss.resteasy.plugins.server.BaseHttpRequest;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.NotImplementedYetException;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.util.CaseInsensitiveMap;

import com.microsoft.azure.functions.HttpRequestMessage;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AzureHttpRequest extends BaseHttpRequest {
    protected HttpRequestMessage<Optional<byte[]>> azureRequest;
    protected SynchronousDispatcher dispatcher;
    protected HttpResponse httpResponse;
    protected ResteasyHttpHeaders httpHeaders;
    protected Map<String, Object> attributes = new HashMap<String, Object>();
    protected String httpMethod;
    protected InputStream inputStream;

    public AzureHttpRequest(final SynchronousDispatcher dispatcher, final HttpResponse httpResponse,
            final HttpRequestMessage<Optional<byte[]>> azureRequest) {
        this(new ResteasyUriInfo(azureRequest.getUri()), dispatcher, httpResponse, azureRequest);
    }

    public AzureHttpRequest(final ResteasyUriInfo uriInfo, final SynchronousDispatcher dispatcher,
            final HttpResponse httpResponse, final HttpRequestMessage<Optional<byte[]>> azureRequest) {
        super(uriInfo);
        this.dispatcher = dispatcher;
        this.httpResponse = httpResponse;
        this.azureRequest = azureRequest;
        CaseInsensitiveMap<String> h = new CaseInsensitiveMap<>();
        azureRequest.getHeaders().forEach((k, v) -> h.add(k, v));
        this.httpHeaders = new ResteasyHttpHeaders(h);
        this.httpMethod = azureRequest.getHttpMethod().name().toUpperCase();
    }

    @Override
    public MultivaluedMap<String, String> getMutableHeaders() {
        return httpHeaders.getMutableHeaders();
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    @Override
    public InputStream getInputStream() {
        if (inputStream == null && azureRequest.getBody().isPresent()) {
            this.inputStream = new ByteArrayInputStream(azureRequest.getBody().get());
        }
        return inputStream;
    }

    @Override
    public void setInputStream(InputStream stream) {
        this.inputStream = stream;
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public void setHttpMethod(String method) {
        this.httpMethod = method;
    }

    @Override
    public Object getAttribute(String attribute) {
        return attributes.get(attribute);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Enumeration<String> en = new Enumeration<String>() {
            private Iterator<String> it = attributes.keySet().iterator();

            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public String nextElement() {
                return it.next();
            }
        };
        return en;
    }

    @Override
    public ResteasyAsynchronousContext getAsyncContext() {
        return new SynchronousExecutionContext(dispatcher, this, httpResponse);
    }

    @Override
    public void forward(String path) {
        throw new NotImplementedYetException();
    }

    @Override
    public boolean wasForwarded() {
        return false;
    }
}
