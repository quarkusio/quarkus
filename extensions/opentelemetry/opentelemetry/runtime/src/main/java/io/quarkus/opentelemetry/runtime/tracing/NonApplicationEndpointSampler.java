package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;

import org.eclipse.microprofile.config.ConfigProvider;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.quarkus.runtime.configuration.NormalizeRootHttpPathConverter;
import io.smallrye.config.SmallRyeConfig;

public class NonApplicationEndpointSampler implements Sampler {
    private static final SamplingResult NEGATIVE_SAMPLING_RESULT = SamplingResult.create(SamplingDecision.DROP);

    private final Sampler sampler;
    private final String namePattern;

    public NonApplicationEndpointSampler(Sampler sampler) {
        this.sampler = sampler;

        // We don't use the HttpBuildTimeConfig because we don't want to add a dependency to vertx-http and vertx-http
        // may not even be available.
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        if (config.isPropertyPresent("quarkus.http.root-path")) {
            String rootPath = config.getValue("quarkus.http.root-path", new NormalizeRootHttpPathConverter());
            String nonApplicationRootPath = config.getRawValue("quarkus.http.non-application-root-path");
            // span names don't include the leading slash
            this.namePattern = rootPath.substring(1) + nonApplicationRootPath;
        } else {
            this.namePattern = null;
        }
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
            Attributes attributes, List<LinkData> parentLinks) {
        if (namePattern != null && name.startsWith(namePattern) && spanKind == SpanKind.SERVER) {
            return NEGATIVE_SAMPLING_RESULT;
        }
        return sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return "NonApplicationEndpointBased{/q}";
    }
}
