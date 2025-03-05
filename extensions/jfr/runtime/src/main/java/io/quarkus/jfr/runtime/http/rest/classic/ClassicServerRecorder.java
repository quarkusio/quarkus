package io.quarkus.jfr.runtime.http.rest.classic;

import io.quarkus.jfr.runtime.IdProducer;
import io.quarkus.jfr.runtime.http.AbstractHttpEvent;
import io.quarkus.jfr.runtime.http.rest.RestEndEvent;
import io.quarkus.jfr.runtime.http.rest.RestPeriodEvent;
import io.quarkus.jfr.runtime.http.rest.RestStartEvent;

public class ClassicServerRecorder {

    private final String httpMethod;
    private final String uri;
    private final String resourceClass;
    private final String resourceMethod;
    private final String client;
    private final IdProducer idProducer;
    private RestPeriodEvent durationEvent;

    public ClassicServerRecorder(String httpMethod, String uri, String resourceClass, String resourceMethod,
            String client,
            IdProducer idProducer) {
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.resourceClass = resourceClass;
        this.resourceMethod = resourceMethod;
        this.client = client;
        this.idProducer = idProducer;
    }

    public void recordStartEvent() {

        RestStartEvent startEvent = new RestStartEvent();

        if (startEvent.shouldCommit()) {
            this.setHttpInfo(startEvent);
            startEvent.commit();
        }
    }

    public void recordEndEvent() {

        RestEndEvent endEvent = new RestEndEvent();

        if (endEvent.shouldCommit()) {
            this.setHttpInfo(endEvent);
            endEvent.commit();
        }
    }

    public void startPeriodEvent() {
        durationEvent = new RestPeriodEvent();
        durationEvent.begin();
    }

    public void endPeriodEvent() {

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
