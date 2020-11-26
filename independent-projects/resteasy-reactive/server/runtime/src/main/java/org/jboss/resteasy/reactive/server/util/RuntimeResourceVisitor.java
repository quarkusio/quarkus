package org.jboss.resteasy.reactive.server.util;

import java.util.List;
import java.util.Map;
import org.jboss.resteasy.reactive.server.handlers.ClassRoutingHandler;
import org.jboss.resteasy.reactive.server.handlers.QuarkusRestInitialHandler;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.spi.RestHandler;

public interface RuntimeResourceVisitor {

    public default void visitBasePath(String basePath) {
    }

    public default void visitRuntimeResource(String httpMethod, String fullPath, RuntimeResource runtimeResource) {
    }

    public default void visitEnd() {
    }

    public static void visitRuntimeResources(
            List<RequestMapper.RequestPath<QuarkusRestInitialHandler.InitialMatch>> classMappers,
            RuntimeResourceVisitor visitor) {
        for (RequestMapper.RequestPath<QuarkusRestInitialHandler.InitialMatch> classMapper : classMappers) {
            String template = classMapper.template.template;
            QuarkusRestInitialHandler.InitialMatch initialMatch = classMapper.value;
            if ((initialMatch.handlers == null) || initialMatch.handlers.length == 0) {
                continue;
            }
            RestHandler firstHandler = initialMatch.handlers[0];
            if (!(firstHandler instanceof ClassRoutingHandler)) {
                continue;
            }
            ClassRoutingHandler classRoutingHandler = (ClassRoutingHandler) firstHandler;

            Map<String, RequestMapper<RuntimeResource>> classRoutingHandlerMappers = classRoutingHandler.getMappers();
            for (Map.Entry<String, RequestMapper<RuntimeResource>> entry : classRoutingHandlerMappers.entrySet()) {
                String basePath = template;
                String httpMethod = entry.getKey();
                if (httpMethod == null) {
                    continue; // TODO: fix as to use all methods
                }

                RequestMapper<RuntimeResource> requestMapper = entry.getValue();
                List<RequestMapper.RequestPath<RuntimeResource>> methodTemplates = requestMapper.getTemplates();
                if (methodTemplates.isEmpty()) {
                    continue;
                }
                visitor.visitBasePath(basePath);
                for (RequestMapper.RequestPath<RuntimeResource> methodTemplate : methodTemplates) {
                    String subPath = methodTemplate.template.template;
                    if (subPath.startsWith("/")) {
                        subPath = subPath.substring(1);
                    }
                    String fullPath = basePath;
                    if (!subPath.isEmpty()) {
                        if (basePath.endsWith("/")) {
                            fullPath += subPath;
                        } else {
                            fullPath = basePath + "/" + subPath;
                        }
                    }
                    RuntimeResource runtimeResource = methodTemplate.value;
                    visitor.visitRuntimeResource(httpMethod, fullPath, runtimeResource);
                }
            }
        }
        visitor.visitEnd();
    }
}
