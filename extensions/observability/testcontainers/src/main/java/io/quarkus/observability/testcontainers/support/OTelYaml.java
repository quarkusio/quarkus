package io.quarkus.observability.testcontainers.support;

import java.util.List;
import java.util.Map;

public class OTelYaml {
    public Map<String, Receiver> receivers;
    public Map<String, Exporter> exporters;
    public Map<String, Processor> processors;
    public Map<String, Extension> extensions;
    public Service service;

    public static class Receiver {
        public Map<String, Protocol> protocols;
    }

    public static class Protocol {
        public String endpoint;
    }

    public static class Exporter {
        public String endpoint;
        public Tls tls;
        public String loglevel;
        public String namespace;
    }

    public static class Tls {
        public boolean insecure;
    }

    public static class Processor {
    }

    public static class Extension {
    }

    public static class Service {
        public List<String> extensions;
        public Map<String, Pipeline> pipelines;
    }

    public static class Pipeline {
        public List<String> receivers;
        public List<String> processors;
        public List<String> exporters;
    }

}
