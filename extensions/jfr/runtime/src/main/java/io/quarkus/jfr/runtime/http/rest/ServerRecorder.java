package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.IdProducer;
import io.quarkus.jfr.runtime.http.AbstractHttpEvent;

public class ServerRecorder implements Recorder {

    private final String httpMethod;
    private final String uri;
    private final String resourceClass;
    private final String resourceMethod;
    private final String client;
    private final IdProducer idProducer;
    private RestPeriodEvent durationEvent;

    public ServerRecorder(String httpMethod, String uri, String resourceClass, String resourceMethod, String client,
            IdProducer idProducer) {
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.resourceClass = resourceClass;
        this.resourceMethod = resourceMethod;
        this.client = client;
        this.idProducer = idProducer;
    }

    @Override
    public void recordStartEvent() {

        RestStartEvent startEvent = new RestStartEvent();

        if (startEvent.shouldCommit()) {
            this.setHttpInfo(startEvent);
            startEvent.commit();
        }
    }

    @Override
    public void recordEndEvent() {

        RestEndEvent endEvent = new RestEndEvent();

        if (endEvent.shouldCommit()) {
            this.setHttpInfo(endEvent);
            endEvent.commit();
        }
    }

    @Override
    public void startPeriodEvent() {
        durationEvent = new RestPeriodEvent();
        durationEvent.begin();
    }

    @Override
    public void endPeriodEvent() {
        if(durationEvent == null) {
            return;
        }
        durationEvent.end();

        if (durationEvent.shouldCommit()) {
            this.setHttpInfo(durationEvent);
            durationEvent.commit();
        }
    }

    private void setHttpInfo(AbstractHttpEvent event) {
        event.setTraceId(idProducer.getTraceId());
        event.setSpanId(idProducer.getSpanId());
        event.setHttpMethod(httpMethod);
        event.setUri(uri);
        event.setResourceClass(resourceClass);
        event.setResourceMethod(resourceMethod);
        event.setClient(client);
    }
}
