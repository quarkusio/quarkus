package io.quarkus.grpc.auth;

import static io.quarkus.grpc.runtime.GrpcServerRecorder.GrpcServiceDefinition.getImplementationClassName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;

import io.grpc.BindableService;
import io.grpc.ServerMethodDefinition;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.grpc.runtime.GrpcContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

@Recorder
public class GrpcSecurityRecorder {

    public void initGrpcSecurityInterceptor(Map<String, List<String>> serviceClassToBlockingMethod,
            BeanContainer container) {

        // service to full method names
        var svcToMethods = new HashMap<String, List<String>>();
        var services = container.beanInstance(GrpcContainer.class).getServices();
        for (BindableService service : services) {
            var className = getImplementationClassName(service);
            var blockingMethods = serviceClassToBlockingMethod.get(className);
            if (blockingMethods != null && !blockingMethods.isEmpty()) {
                var svcName = service.bindService().getServiceDescriptor().getName();
                var methods = new ArrayList<String>();
                for (String blockingMethod : blockingMethods) {
                    for (ServerMethodDefinition<?, ?> method : service.bindService().getMethods()) {
                        if (blockingMethod.equals(method.getMethodDescriptor().getBareMethodName())) {
                            methods.add(method.getMethodDescriptor().getFullMethodName());
                            break;
                        }
                    }
                }
                svcToMethods.put(svcName, methods);
            }
        }

        container.beanInstance(GrpcSecurityInterceptor.class).init(svcToMethods);
    }

    public void validateSecurityEventsDisabled(String observedSecurityEvent) {
        boolean securityEventsEnabled = ConfigProvider
                .getConfig()
                .getOptionalValue("quarkus.security.events.enabled", boolean.class)
                .orElse(Boolean.TRUE);
        if (securityEventsEnabled) {
            throw new ConfigurationException("""
                    Found observer method for event type '%s', but the gRPC extension does not support security
                    events. Either disable security events with the 'quarkus.security.events.enabled'
                    configuration property, or remove security event CDI observers.""".formatted(observedSecurityEvent),
                    Set.of("quarkus.security.events.enabled"));
        }
    }
}
