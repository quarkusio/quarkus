package io.quarkus.azure.functions.resteasy.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import io.quarkus.runtime.Application;

public class Function {

    public HttpResponseMessage run(
            @HttpTrigger(name = "req") HttpRequestMessage<Optional<byte[]>> request,
            final ExecutionContext context) {
        if (!started) {
            HttpResponseMessage.Builder responseBuilder = request
                    .createResponseBuilder(HttpStatus.valueOf(500)).body(
                            deploymentStatus.getBytes());
        }
        return resteasyDispatch(request);
    }

    static final String deploymentStatus;
    static boolean started = false;

    static {
        StringWriter error = new StringWriter();
        PrintWriter errorWriter = new PrintWriter(error, true);
        try {
            Class appClass = Class.forName("io.quarkus.runner.ApplicationImpl1");
            String[] args = {};
            Application app = (Application) appClass.newInstance();
            app.start(args);
            errorWriter.println("Quarkus bootstrapped successfully.");
            started = true;
        } catch (Exception ex) {
            errorWriter.println("Quarkus bootstrap failed.");
            ex.printStackTrace(errorWriter);
        }
        deploymentStatus = error.toString();
    }

    protected HttpResponseMessage resteasyDispatch(HttpRequestMessage<Optional<byte[]>> request) {
        MockHttpResponse httpResponse = new MockHttpResponse();
        String contextPath = "/api";
        String rootPath = AzureFunctionsResteasyTemplate.rootPath;
        if (rootPath != null && rootPath.length() > 0 && !"/".equals(rootPath)) {
            contextPath = rootPath;
        }
        ResteasyUriInfo uriInfo = new ResteasyUriInfo(request.getUri().toString(), request.getUri().getRawQuery(), contextPath);
        AzureHttpRequest httpRequest = new AzureHttpRequest(uriInfo,
                (SynchronousDispatcher) AzureFunctionsResteasyTemplate.deployment.getDispatcher(),
                httpResponse, request);
        AzureFunctionsResteasyTemplate.deployment.getDispatcher().invoke(httpRequest, httpResponse);
        HttpResponseMessage.Builder responseBuilder = request
                .createResponseBuilder(HttpStatus.valueOf(httpResponse.getStatus()));
        for (Map.Entry<String, List<Object>> entry : httpResponse.getOutputHeaders().entrySet()) {
            for (Object o : entry.getValue()) {
                RuntimeDelegate.HeaderDelegate delegate = AzureFunctionsResteasyTemplate.deployment.getProviderFactory()
                        .getHeaderDelegate(o.getClass());
                if (delegate != null) {
                    responseBuilder.header(entry.getKey(), delegate.toString(o));
                } else {
                    responseBuilder.header(entry.getKey(), o.toString());
                }
            }
        }
        responseBuilder.body(httpResponse.getOutput());
        return responseBuilder.build();
    }
}
