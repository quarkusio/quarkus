package io.quarkus.resteasy.reactive.server.runtime.devui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.resteasy.reactive.server.core.RuntimeExceptionMapper;
import org.jboss.resteasy.reactive.server.util.ScoreSystem;

import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRecorder;

public class ResteasyReactiveJsonRPCService {

    public ScoreSystem.EndpointScores getEndpointScores() {
        ScoreSystem.EndpointScores result = ScoreSystem.latestScores;
        if (result != null) {
            return result;
        }

        return new ScoreSystem.EndpointScores(0, Collections.emptyList());
    }

    public List<ResteasyReactiveExceptionMapper> getExceptionMappers() {
        List<ResteasyReactiveExceptionMapper> all = new ArrayList<>();
        var mappers = RuntimeExceptionMapper.getMappers();
        for (var entry : mappers.entrySet()) {
            all.add(new ResteasyReactiveExceptionMapper(entry.getKey().getName(), entry.getValue().getClassName(),
                    entry.getValue().getPriority()));
        }
        return all;
    }

    public List<ResteasyReactiveParamConverterProvider> getParamConverterProviders() {
        List<ResteasyReactiveParamConverterProvider> all = new ArrayList<>();
        var providers = ResteasyReactiveRecorder.getCurrentDeployment().getParamConverterProviders()
                .getParamConverterProviders();
        for (var provider : providers) {
            all.add(new ResteasyReactiveParamConverterProvider(provider.getClassName(),
                    provider.getPriority()));
        }
        return all;
    }
}
