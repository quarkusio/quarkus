package io.quarkus.rest.runtime.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.rest.runtime.handlers.ClassRoutingHandler;
import io.quarkus.rest.runtime.handlers.QuarkusRestInitialHandler;
import io.quarkus.rest.runtime.handlers.QuarkusRestInitialHandler.InitialMatch;
import io.quarkus.rest.runtime.handlers.RestHandler;
import io.quarkus.rest.runtime.mapping.RequestMapper;
import io.quarkus.rest.runtime.mapping.RequestMapper.RequestPath;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
import io.quarkus.rest.runtime.model.ResourceWriter;

public class ScoreSystem {

    public static class Diagnostic {

        private String message;
        private int percentageScore;

        public Diagnostic(String message, int percentageScore) {
            this.message = message;
            this.percentageScore = percentageScore;
        }

        @Override
        public String toString() {
            return message + ": " + percentageScore + "/100";
        }

        public static Diagnostic ExecutionNonBlocking = new Diagnostic("Dispatched on the IO thread", 100);
        public static Diagnostic ExecutionBlocking = new Diagnostic("Needs a worker thread dispatch", 0);

        public static Diagnostic ResourceSingleton = new Diagnostic("Single resource instance for all requests", 100);
        public static Diagnostic ResourcePerRequest = new Diagnostic("New resource instance for every request", 0);

        public static Diagnostic WriterBuildTime(MessageBodyWriter<?> buildTimeWriter) {
            return new Diagnostic("Single writer set at build time: " + buildTimeWriter, 100);
        }

        public static Diagnostic WriterBuildTimeMultiple(List<MessageBodyWriter<?>> buildTimeWriters) {
            return new Diagnostic("Multiple writers set at build time: " + buildTimeWriters, 50);
        }

        public static Diagnostic WriterRunTime = new Diagnostic("Run time writers required", 0);

    }

    public enum Category {
        Writer,
        Resource,
        Execution
    }

    public static void dumpScores(List<RequestPath<InitialMatch>> classMappers) {
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
                    ServerMediaType serverMediaType = runtimeResource.getProduces();
                    List<MediaType> produces = Collections.emptyList();
                    if (serverMediaType != null) {
                        if ((serverMediaType.getSortedOriginalMediaTypes() != null)
                                && serverMediaType.getSortedOriginalMediaTypes().length >= 1) {
                            produces = Arrays.asList(serverMediaType.getSortedOriginalMediaTypes());
                        }
                    }
                    System.err.println(httpMethod + " " + fullPath);
                    for (RestHandler handler : runtimeResource.getHandlerChain()) {
                        System.err.println(" " + handler);
                    }
                    if (!produces.isEmpty()) {
                        System.err.println(" Produces: " + produces);
                    }
                    List<MediaType> consumes = runtimeResource.getConsumes();
                    if (!consumes.isEmpty()) {
                        System.err.println(" Consumes: " + consumes);
                    }
                    System.err.println(" Diagnostics:");
                    int overall = 0;
                    int total = 0;
                    for (Entry<Category, List<Diagnostic>> score : runtimeResource.getScore().entrySet()) {
                        System.err.println("  " + score.getKey() + ": " + score.getValue());
                        for (Diagnostic diagnostic : score.getValue()) {
                            overall += diagnostic.percentageScore;
                        }
                        total += 100;
                    }
                    // let's bring it to 100
                    overall = (int) Math.floor(((float) overall / (float) total) * 100f);
                    System.err.println(" Score: " + overall + "/100");
                }
            }
        }
    }

}
