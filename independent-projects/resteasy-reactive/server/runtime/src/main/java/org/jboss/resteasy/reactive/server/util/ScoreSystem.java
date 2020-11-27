package org.jboss.resteasy.reactive.server.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.spi.RestHandler;

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
            return new Diagnostic("Single writer set at build time: " + buildTimeWriter, 90);
        }

        public static Diagnostic WriterBuildTimeDirect(MessageBodyWriter<?> buildTimeWriter) {
            return new Diagnostic("Single direct writer set at build time: " + buildTimeWriter, 100);
        }

        public static Diagnostic WriterBuildTimeMultiple(List<MessageBodyWriter<?>> buildTimeWriters) {
            return new Diagnostic("Multiple writers set at build time: " + buildTimeWriters, 50);
        }

        public static Diagnostic WriterRunTime = new Diagnostic("Run time writers required", 0);
        public static Diagnostic WriterNotRequired = new Diagnostic("No writers required", 100);

    }

    public enum Category {
        Writer,
        Resource,
        Execution
    }

    public final static RuntimeResourceVisitor ScoreVisitor = new RuntimeResourceVisitor() {
        int overallScore = 0;
        int overallTotal = 0;

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
            if (runtimeResource.getScore() == null) {
                System.err.println(" Unable to determine score");
                return;
            }
            int score = 0;
            int total = 0;
            for (Entry<Category, List<Diagnostic>> scoreEntry : runtimeResource.getScore().entrySet()) {
                System.err.println("  " + scoreEntry.getKey() + ": " + scoreEntry.getValue());
                for (Diagnostic diagnostic : scoreEntry.getValue()) {
                    score += diagnostic.percentageScore;
                }
                total += 100;
            }
            // let's bring it to 100
            score = (int) Math.floor(((float) score / (float) total) * 100f);
            overallScore += score;
            overallTotal += 100;
            System.err.println(" Score: " + score + "/100");
        }

        @Override
        public void visitEnd() {
            if (overallScore == 0) {
                // we were most likely not able to determine the score, so don't print anything as it will be misleading
                return;
            }
            // let's bring it to 100
            overallScore = (int) Math.floor(((float) overallScore / (float) overallTotal) * 100f);
            System.err.println("Overall Score: " + overallScore + "/100");
        }
    };
}
