package io.quarkus.observability.testcontainers.test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.quarkus.observability.testcontainers.support.OTelYaml;

public class OTelYamlTest {

    @Test
    public void testYaml() throws Exception {
        YAMLMapper yaml = new YAMLMapper();
        yaml.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        yaml.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        String config;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("otel-collector-config-template.yaml")) {
            OTelYaml otelYaml = yaml.readValue(is, OTelYaml.class);
            // processor, extension
            otelYaml.processors.put("batch", new OTelYaml.Processor());
            otelYaml.extensions.put("health_check", new OTelYaml.Extension());
            // exporter
            OTelYaml.Exporter exporter1 = new OTelYaml.Exporter();
            exporter1.endpoint = "jaeger:14250";
            OTelYaml.Tls tls = new OTelYaml.Tls();
            tls.insecure = true;
            exporter1.tls = tls;
            otelYaml.exporters.put("jaeger", exporter1);
            // service
            OTelYaml.Pipeline pipeline = new OTelYaml.Pipeline();
            pipeline.receivers = List.of("otlp");
            pipeline.processors = List.of("batch");
            pipeline.exporters = List.of("jaeger");
            otelYaml.service.pipelines.put("traces", pipeline);
            // exporter
            OTelYaml.Exporter exporter2 = new OTelYaml.Exporter();
            exporter2.endpoint = "victoria-metrics:8428";
            exporter2.namespace = "quarkus_observability";
            otelYaml.exporters.put("prometheus", exporter2);
            // service
            OTelYaml.Pipeline metrics = otelYaml.service.pipelines.get("metrics");
            List<String> exs = metrics.exporters;
            List<String> newExs = new ArrayList<>(exs);
            newExs.add("prometheus");
            metrics.exporters = newExs;
            // dump
            config = yaml.writeValueAsString(otelYaml);
        }
        System.out.println(config);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("otel-collector-config.yaml")) {
            OTelYaml oTelYaml = yaml.readValue(is, OTelYaml.class);
            System.out.println(yaml.writeValueAsString(oTelYaml));
        }
    }
}
