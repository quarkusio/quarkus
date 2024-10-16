package io.quarkus.grpc.auth;

import static io.quarkus.grpc.runtime.GrpcServerRecorder.GrpcServiceDefinition.getImplementationClassName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.grpc.BindableService;
import io.grpc.ServerMethodDefinition;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.grpc.runtime.GrpcContainer;
import io.quarkus.runtime.annotations.Recorder;

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
}
