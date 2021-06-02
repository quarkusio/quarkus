package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;

import org.jboss.logging.Logger;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

/**
 * Class enabling Quarkus to instantiate a {@link io.opentelemetry.api.trace.TracerProvider}
 * during static initialization and set a {@link Sampler} delegate during runtime initialization.
 */
public class LateBoundSampler implements Sampler {
    private static final Logger log = Logger.getLogger(LateBoundSampler.class);
    private boolean warningLogged = false;

    private Sampler delegate;

    /**
     * Set the actual {@link Sampler} to use as the delegate.
     *
     * @param delegate Properly constructed {@link Sampler}.
     */
    public void setSamplerDelegate(Sampler delegate) {
        this.delegate = delegate;
    }

    @Override
    public SamplingResult shouldSample(Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks) {
        if (delegate == null) {
            logDelegateNotFound();
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
        }
        return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        if (delegate == null) {
            logDelegateNotFound();
            return "";
        }
        return delegate.getDescription();
    }

    /**
     * If we haven't previously logged an error,
     * log an error about a missing {@code delegate} and set {@code warningLogged=true}
     */
    private void logDelegateNotFound() {
        if (!warningLogged) {
            log.warn("No Sampler delegate specified, no action taken.");
            warningLogged = true;
        }
    }
}
