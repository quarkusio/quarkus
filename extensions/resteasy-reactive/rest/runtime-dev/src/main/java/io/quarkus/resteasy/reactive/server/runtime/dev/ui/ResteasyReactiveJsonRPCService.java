package io.quarkus.resteasy.reactive.server.runtime.dev.ui;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.core.RuntimeExceptionMapper;
import org.jboss.resteasy.reactive.server.util.ScoreSystem;

import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRecorder;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResteasyReactiveJsonRPCService {

    @NonBlocking
    public JsonObject getEndpointScores() {
        JsonObject endpointScore = new JsonObject();

        ScoreSystem.EndpointScores result = ScoreSystem.latestScores;
        if (result != null) {
            endpointScore.put("score", result.score);
            JsonArray endpoints = new JsonArray();
            for (ScoreSystem.EndpointScore endpoint : result.endpoints) {
                JsonObject e = new JsonObject();
                e.put("className", endpoint.className);
                e.put("httpMethod", endpoint.httpMethod);
                e.put("fullPath", endpoint.fullPath);
                e.put("producesHeaders", endpoint.produces.stream()
                        .map(MediaType::toString)
                        .collect(Collectors.toList()));
                e.put("consumesHeaders", endpoint.consumes.stream()
                        .map(MediaType::toString)
                        .collect(Collectors.toList()));

                JsonObject diagnostics = new JsonObject();
                Map<ScoreSystem.Category, List<ScoreSystem.Diagnostic>> sortedDiagnostics = new TreeMap<>(endpoint.diagnostics);
                for (Map.Entry<ScoreSystem.Category, List<ScoreSystem.Diagnostic>> diagnostic : sortedDiagnostics.entrySet()) {
                    JsonArray diagnosticValues = new JsonArray();
                    for (ScoreSystem.Diagnostic value : diagnostic.getValue()) {
                        JsonObject diagnosticValue = new JsonObject();
                        diagnosticValue.put("message", value.message);
                        diagnosticValue.put("score", value.score);
                        diagnosticValues.add(diagnosticValue);
                    }
                    diagnostics.put(diagnostic.getKey().name(), diagnosticValues);
                }
                e.put("diagnostics", diagnostics);
                e.put("requestFilterEntries", endpoint.requestFilterEntries.stream()
                        .map(ScoreSystem.RequestFilterEntry::getName)
                        .collect(Collectors.toList()));
                e.put("score", endpoint.score);
                endpoints.add(e);
            }
            endpointScore.put("endpoints", endpoints);
        } else {
            endpointScore.put("score", 0);
        }

        return endpointScore;
    }

    @NonBlocking
    public JsonArray getExceptionMappers() {
        JsonArray all = new JsonArray();
        var mappers = RuntimeExceptionMapper.getMappers();
        for (var entry : mappers.entrySet()) {
            JsonObject m = new JsonObject();
            m.put("name", entry.getKey().getName());
            m.put("className", entry.getValue().getClassName());
            m.put("priority", entry.getValue().getPriority());
            all.add(m);
        }
        return all;
    }

    @NonBlocking
    public JsonArray getParamConverterProviders() {
        JsonArray all = new JsonArray();
        var providers = ResteasyReactiveRecorder.getCurrentDeployment().getParamConverterProviders()
                .getParamConverterProviders();
        for (var provider : providers) {
            JsonObject m = new JsonObject();
            m.put("className", provider.getClassName());
            m.put("priority", provider.getPriority());
            all.add(m);
        }
        return all;
    }
}
