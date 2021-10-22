package org.jboss.resteasy.reactive.server.model;

import org.jboss.resteasy.reactive.server.handlers.PublisherResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseHandler;

public class FixedResponseBuilderAndStreamingResponseCustomizer implements HandlerChainCustomizer {

    private ResponseHandler.ResponseBuilderCustomizer responseBuilderCustomizer;
    private PublisherResponseHandler.StreamingResponseCustomizer streamingResponseCustomizer;

    public FixedResponseBuilderAndStreamingResponseCustomizer() {
    }

    public FixedResponseBuilderAndStreamingResponseCustomizer(
            ResponseHandler.ResponseBuilderCustomizer responseBuilderCustomizer,
            PublisherResponseHandler.StreamingResponseCustomizer streamingResponseCustomizer) {
        this.responseBuilderCustomizer = responseBuilderCustomizer;
        this.streamingResponseCustomizer = streamingResponseCustomizer;
    }

    public ResponseHandler.ResponseBuilderCustomizer getResponseBuilderCustomizer() {
        return responseBuilderCustomizer;
    }

    public void setResponseBuilderCustomizer(ResponseHandler.ResponseBuilderCustomizer responseBuilderCustomizer) {
        this.responseBuilderCustomizer = responseBuilderCustomizer;
    }

    public PublisherResponseHandler.StreamingResponseCustomizer getStreamingResponseCustomizer() {
        return streamingResponseCustomizer;
    }

    public void setStreamingResponseCustomizer(
            PublisherResponseHandler.StreamingResponseCustomizer streamingResponseCustomizer) {
        this.streamingResponseCustomizer = streamingResponseCustomizer;
    }

    @Override
    public ResponseHandler.ResponseBuilderCustomizer successfulInvocationResponseBuilderCustomizer(
            ServerResourceMethod method) {
        return responseBuilderCustomizer;
    }

    @Override
    public PublisherResponseHandler.StreamingResponseCustomizer streamingResponseCustomizer(ServerResourceMethod method) {
        return streamingResponseCustomizer;
    }
}
