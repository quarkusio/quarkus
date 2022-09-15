package org.jboss.resteasy.reactive.server.util;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.server.handlers.ResourceRequestFilterHandler;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class ScoreSystem {

    public static class EndpointScores {

        public final List<EndpointScore> endpoints;
        public final int score;

        public EndpointScores(int score, List<EndpointScore> endpoints) {
            this.score = score;
            this.endpoints = endpoints;
        }

    }

    public static class EndpointScore {

        public final String className;
        public final String httpMethod;
        public final String fullPath;
        public final List<MediaType> produces;
        public final List<MediaType> consumes;
        public final Map<Category, List<Diagnostic>> diagnostics;
        public final int score;
        public final List<RequestFilterEntry> requestFilterEntries;

        public EndpointScore(String className, String httpMethod, String fullPath, List<MediaType> produces,
                List<MediaType> consumes,
                Map<Category, List<Diagnostic>> diagnostics, int score, List<RequestFilterEntry> requestFilterEntries) {
            this.className = className;
            this.httpMethod = httpMethod;
            this.fullPath = fullPath;
            this.produces = produces;
            this.consumes = consumes;
            this.diagnostics = diagnostics;
            this.score = score;
            this.requestFilterEntries = requestFilterEntries;
        }

    }

    public static class RequestFilterEntry {
        public final String name;
        public final boolean preMatch;

        public String getName() {
            String removeSubClass = name.replace("_Subclass", "");
            String finalFilterName = removeSubClass.replaceAll("\\$.*?\\$", "::");
            return finalFilterName;
        }

        public RequestFilterEntry(String name, boolean preMatch) {
            this.name = name;
            this.preMatch = preMatch;
        }
    }

    public static class Diagnostic {

        public final String message;
        public final int score;

        public Diagnostic(String message, int percentageScore) {
            this.message = message;
            this.score = percentageScore;
        }

        @Override
        public String toString() {
            return message + ": " + score + "/100";
        }

        public static Diagnostic ExecutionNonBlocking = new Diagnostic("Dispatched on the IO thread", 100);
        public static Diagnostic ExecutionBlocking = new Diagnostic("Needs a worker thread dispatch", 0);

        public static Diagnostic ResourceSingleton = new Diagnostic("Single resource instance for all requests", 100);
        public static Diagnostic ResourcePerRequest = new Diagnostic("New resource instance for every request", 0);

        public static Diagnostic WriterBuildTime(MessageBodyWriter<?> buildTimeWriter) {
            return new Diagnostic("Single writer set at build time: " + buildTimeWriter.getClass().getName(), 90);
        }

        public static Diagnostic WriterBuildTimeDirect(MessageBodyWriter<?> buildTimeWriter) {
            return new Diagnostic("Single direct writer set at build time: " + buildTimeWriter.getClass().getName(), 100);
        }

        public static Diagnostic WriterBuildTimeMultiple(List<MessageBodyWriter<?>> buildTimeWriters) {
            return new Diagnostic("Multiple writers set at build time: [" + getClassNames(buildTimeWriters) + "]", 50);
        }

        private static String getClassNames(List<MessageBodyWriter<?>> buildTimeWriters) {
            List<String> classNames = new ArrayList<>(buildTimeWriters.size());
            for (MessageBodyWriter<?> buildTimeWriter : buildTimeWriters) {
                classNames.add(buildTimeWriter.getClass().getName());
            }
            return String.join(", ", classNames);
        }

        public static Diagnostic WriterRunTime = new Diagnostic("Run time writers required", 0);
        public static Diagnostic WriterNotRequired = new Diagnostic("No writers required", 100);

    }

    public enum Category {
        Writer,
        Resource,
        Execution
    }

    public static EndpointScores latestScores;

    public final static RuntimeResourceVisitor ScoreVisitor = new RuntimeResourceVisitor() {
        int overallScore = 0;
        int overallTotal = 0;
        final List<EndpointScore> endpoints = new ArrayList<>();

        @Override
        public void visitRuntimeResource(String httpMethod, String fullPath, RuntimeResource runtimeResource) {
            ServerMediaType serverMediaType = runtimeResource.getProduces();
            List<MediaType> produces = Collections.emptyList();
            if (serverMediaType != null) {
                if ((serverMediaType.getSortedOriginalMediaTypes() != null)
                        && serverMediaType.getSortedOriginalMediaTypes().length >= 1) {
                    produces = Arrays.asList(serverMediaType.getSortedOriginalMediaTypes());
                }
            }

            ServerRestHandler[] handlerChain = runtimeResource.getHandlerChain();
            List<RequestFilterEntry> requestFilters = new ArrayList<>();
            for (ServerRestHandler serverRestHandler : handlerChain) {
                if (serverRestHandler instanceof ResourceRequestFilterHandler) {
                    ResourceRequestFilterHandler requestFilterHandler = (ResourceRequestFilterHandler) serverRestHandler;
                    requestFilters.add(new RequestFilterEntry(requestFilterHandler.getFilter().getClass().getName(),
                            requestFilterHandler.isPreMatch()));
                }
            }
            List<MediaType> consumes = runtimeResource.getConsumes();
            if (runtimeResource.getScore() == null) {
                return;
            }
            int score = 0;
            int total = 0;
            for (Entry<Category, List<Diagnostic>> scoreEntry : runtimeResource.getScore().entrySet()) {
                for (Diagnostic diagnostic : scoreEntry.getValue()) {
                    score += diagnostic.score;
                }
                total += 100;
            }
            // let's bring it to 100
            score = (int) Math.floor(((float) score / (float) total) * 100f);
            overallScore += score;
            overallTotal += 100;
            endpoints.add(new EndpointScore(runtimeResource.getResourceClass().getName(), httpMethod, fullPath, produces,
                    consumes, runtimeResource.getScore(), score,
                    requestFilters));
        }

        @Override
        public void visitEnd() {
            if (overallScore == 0) {
                // we were most likely not able to determine the score, so don't print anything as it will be misleading
                return;
            }
            // let's bring it to 100
            overallScore = (int) Math.floor(((float) overallScore / (float) overallTotal) * 100f);
            latestScores = new EndpointScores(overallScore, endpoints);
        }

        @Override
        public void visitStart() {
            //clear the endpoints
            endpoints.clear();
            //reset overallScore and overallTotal
            overallScore = 0;
            overallTotal = 0;
        }
    };
}
