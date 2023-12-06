package io.quarkus.grpc.runtime.devui;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.runtime.devmode.GrpcServices;
import io.quarkus.vertx.http.runtime.CertificateConfig;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * We should consider to use gRPC directly from the Javascript client.
 * At the moment we send the data over json-rpc (web socket) to just create a Java gRPC client that calls the gRPC server
 * method.
 * We can just call the server method directly from Javascript.
 * See @grpc/grpc-js
 */
public class GrpcJsonRPCService {
    private static final Logger LOG = Logger.getLogger(GrpcJsonRPCService.class);

    @Inject
    HttpConfiguration httpConfiguration;

    @Inject
    HttpBuildTimeConfig httpBuildTimeConfig;

    @Inject
    GrpcConfiguration grpcConfiguration;

    @Inject
    GrpcServices grpcServices;

    private String host;
    private int port;
    private boolean ssl;

    @PostConstruct
    public void init() {
        GrpcServerConfiguration serverConfig = grpcConfiguration.server;
        if (serverConfig.useSeparateServer) {
            this.host = serverConfig.host;
            this.port = serverConfig.port;
            this.ssl = serverConfig.ssl.certificate.isPresent() || serverConfig.ssl.keyStore.isPresent();
        } else {
            this.host = httpConfiguration.host;
            this.port = httpConfiguration.port;
            this.ssl = isTLSConfigured(httpConfiguration.ssl.certificate);
        }
    }

    private boolean isTLSConfigured(CertificateConfig certificate) {
        return certificate.files.isPresent()
                || certificate.keyFiles.isPresent()
                || certificate.keyStoreFile.isPresent();
    }

    public JsonArray getServices() {
        JsonArray services = new JsonArray();
        List<GrpcServices.ServiceDefinitionAndStatus> infos = this.grpcServices.getInfos();

        for (GrpcServices.ServiceDefinitionAndStatus info : infos) {
            JsonObject service = new JsonObject();
            service.put("status", info.status);
            service.put("name", info.getName());
            service.put("serviceClass", info.getServiceClass());
            service.put("hasTestableMethod", info.hasTestableMethod());

            JsonArray methods = new JsonArray();
            for (GrpcServices.MethodAndPrototype methodAndPrototype : info.getMethodsWithPrototypes()) {
                JsonObject method = new JsonObject();
                method.put("bareMethodName", methodAndPrototype.getBareMethodName());
                method.put("type", methodAndPrototype.getType());
                method.put("prototype", methodAndPrototype.getPrototype());
                method.put("isTestable", methodAndPrototype.isTestable());
                methods.add(method);
            }
            service.put("methods", methods);
            services.add(service);
        }

        return services;
    }

    public Uni<String> testService(String serviceName, String methodName, String methodType, String content) {
        try {
            return streamService(serviceName, methodName, methodType, content).toUni();
        } catch (Throwable t) {
            return Uni.createFrom().item(error(t.getMessage()).encodePrettily());
        }
    }

    public Multi<String> streamService(String serviceName, String methodName, String methodType, String content) {
        if (content == null) {
            return Multi.createFrom().item(error("Invalid messsge").encodePrettily());
        }
        Map<String, String> params = createParams(serviceName, methodName, methodType, content);
        Flow.Publisher<String> publisher = DevConsoleManager.invoke("grpc-action", params);
        return Multi.createFrom().publisher(publisher);
        //return multi.onItem().transform((json) -> new JsonObject(json));
    }

    private JsonObject error(String message) {
        LOG.error(message);
        JsonObject error = new JsonObject();
        error.put("status", "ERROR");
        error.put("message", message);
        return error;
    }

    private Map<String, String> createParams(String serviceName, String methodName, String methodType, String content) {
        return Map.of(
                "serviceName", serviceName,
                "methodName", methodName,
                "methodType", methodType,
                "content", content,
                "host", host,
                "port", String.valueOf(port),
                "ssl", String.valueOf(ssl));
    }
}
