package io.quarkus.grpc.runtime.devmode;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import grpc.health.v1.HealthOuterClass.HealthCheckResponse.ServingStatus;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.ServerMethodDefinition;
import io.quarkus.arc.Subclass;
import io.quarkus.arc.Unremovable;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.grpc.runtime.GrpcServerRecorder;
import io.quarkus.grpc.runtime.GrpcServerRecorder.GrpcServiceDefinition;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.devmode.GrpcServices.ServiceDefinitionAndStatus;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;

@Unremovable
@Singleton
public class GrpcServices extends AbstractMap<String, ServiceDefinitionAndStatus> {

    @Inject
    GrpcConfiguration configuration;

    @Inject
    GrpcHealthStorage healthStorage;

    @Inject
    DelegatingGrpcBeansStorage delegatingBeansMapping;

    public List<ServiceDefinitionAndStatus> getInfos() {
        List<GrpcServiceDefinition> services = GrpcServerRecorder.getServices();
        List<ServiceDefinitionAndStatus> infos = new ArrayList<>(services.size());
        for (GrpcServiceDefinition service : services) {
            infos.add(new ServiceDefinitionAndStatus(service, healthStorage.getStatuses()
                    .getOrDefault(service.definition.getServiceDescriptor().getName(), ServingStatus.UNKNOWN)));
        }
        return infos;
    }

    @Override
    public Set<Entry<String, ServiceDefinitionAndStatus>> entrySet() {
        Set<Entry<String, ServiceDefinitionAndStatus>> entries = new HashSet<>();
        for (GrpcServiceDefinition definition : GrpcServerRecorder.getServices()) {
            entries.add(new ServiceDefinitionAndStatus(definition, healthStorage.getStatuses()
                    .getOrDefault(definition.definition.getServiceDescriptor().getName(), ServingStatus.UNKNOWN)));
        }
        return entries;
    }

    public class ServiceDefinitionAndStatus implements Map.Entry<String, ServiceDefinitionAndStatus> {

        public final GrpcServiceDefinition definition;
        public final ServingStatus status;

        public ServiceDefinitionAndStatus(GrpcServiceDefinition definition, ServingStatus status) {
            this.definition = definition;
            this.status = status;
        }

        public String getName() {
            return definition.definition.getServiceDescriptor().getName();
        }

        public String getServiceClass() {
            Class<?> instanceClass = definition.service.getClass();
            if (definition.service instanceof Subclass) {
                instanceClass = instanceClass.getSuperclass();
            }

            String grpcBeanClassName = instanceClass.getName();

            String userClass = delegatingBeansMapping.getUserClassName(grpcBeanClassName);

            return userClass != null ? userClass : grpcBeanClassName;
        }

        public Collection<ServerMethodDefinition<?, ?>> getMethods() {
            return definition.definition.getMethods();
        }

        public Collection<MethodAndPrototype> getMethodsWithPrototypes() {
            Map<String, String> prototypes = DevConsoleManager.getGlobal("io.quarkus.grpc.messagePrototypes");
            List<MethodAndPrototype> methods = new ArrayList<>();
            for (ServerMethodDefinition<?, ?> method : getMethods()) {
                methods.add(
                        new MethodAndPrototype(method,
                                prototypes.get(method.getMethodDescriptor().getFullMethodName() + "_REQUEST")));
            }
            return methods;
        }

        @Override
        public String getKey() {
            return getName();
        }

        @Override
        public ServiceDefinitionAndStatus getValue() {
            return this;
        }

        @Override
        public ServiceDefinitionAndStatus setValue(ServiceDefinitionAndStatus value) {
            throw new UnsupportedOperationException();
        }

        public boolean hasTestableMethod() {
            if (configuration.server.ssl.certificate.isPresent() || configuration.server.ssl.keyStore.isPresent()) {
                return false;
            }
            Map<String, String> prototypes = DevConsoleManager.getGlobal("io.quarkus.grpc.messagePrototypes");
            for (ServerMethodDefinition<?, ?> method : getMethods()) {
                if (method.getMethodDescriptor().getType() != MethodType.UNKNOWN
                        && prototypes.containsKey(method.getMethodDescriptor().getFullMethodName() + "_REQUEST")) {
                    return true;
                }
            }
            return false;
        }

    }

    public class MethodAndPrototype {

        private final ServerMethodDefinition<?, ?> definition;
        private final String prototype;

        public MethodAndPrototype(ServerMethodDefinition<?, ?> definition, String prototype) {
            this.definition = definition;
            this.prototype = prototype;
        }

        public MethodType getType() {
            return definition.getMethodDescriptor().getType();
        }

        public String getBareMethodName() {
            return definition.getMethodDescriptor().getBareMethodName();
        }

        public String getFullMethodName() {
            return definition.getMethodDescriptor().getFullMethodName();
        }

        public boolean hasPrototype() {
            return prototype != null;
        }

        public boolean isTestable() {
            return configuration.server.ssl.certificate.isEmpty()
                    && configuration.server.ssl.keyStore.isEmpty();
        }

        public String getPrototype() {
            return prototype;
        }

    }

}
