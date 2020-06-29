package io.quarkus.kubernetes.deployment;

final class Annotations {

    private Annotations() {
    }

    static final class Prometheus {

        private Prometheus() {
        }

        private static final String PREFIX = "prometheus.io/";

        static final String SCRAPE = PREFIX + "scrape";
        static final String PATH = PREFIX + "path";
        static final String PORT = PREFIX + "port";
    }
}
