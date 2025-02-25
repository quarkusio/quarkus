package io.quarkus.jfr.runtime.http.rest.reactive;

import io.quarkus.jfr.runtime.IdProducer;
import io.quarkus.jfr.runtime.http.AbstractHttpEvent;
import io.quarkus.jfr.runtime.http.rest.RestEndEvent;
import io.quarkus.jfr.runtime.http.rest.RestPeriodEvent;
import io.quarkus.jfr.runtime.http.rest.RestStartEvent;

class ReactiveServerRecorder {

    private final RequestInfo requestInfo;
    private final IdProducer idProducer;

    private volatile ResourceInfo resourceInfo;

    private volatile RestStartEvent startEvent;
    // TODO: we can perhaps get rid of this volatile if access patterns to this and startEvent allow it
    private volatile boolean startEventHandled;

    private volatile RestPeriodEvent durationEvent;

    public ReactiveServerRecorder(RequestInfo requestInfo, IdProducer idProducer) {
        this.requestInfo = requestInfo;
        this.idProducer = idProducer;
    }

    public ReactiveServerRecorder createStartEvent() {
        startEvent = new RestStartEvent();
        return this;
    }

    public ReactiveServerRecorder createAndStartPeriodEvent() {
        durationEvent = new RestPeriodEvent();
        durationEvent.begin();
        return this;
    }

    public ReactiveServerRecorder updateResourceInfo(ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
        return this;
    }

    public ReactiveServerRecorder commitStartEventIfNecessary() {
        startEventHandled = true;
        var se = startEvent;
        if (se.shouldCommit()) {
            setHttpInfo(startEvent);
            se.commit();
        }
        return this;
    }

    /**
     * Because this can be called when a start event has not been completely handled
     * (this happens when request processing failed because a Resource method could not be identified),
     * we need to handle that event as well.
     */
    public ReactiveServerRecorder recordEndEvent() {
        if (!startEventHandled) {
            commitStartEventIfNecessary();
        }

        RestEndEvent endEvent = new RestEndEvent();
        if (endEvent.shouldCommit()) {
            setHttpInfo(endEvent);
            endEvent.commit();
        }

        return this;
    }

    public ReactiveServerRecorder endPeriodEvent() {
        if (durationEvent != null) {
            durationEvent.end();
            if (durationEvent.shouldCommit()) {
                setHttpInfo(durationEvent);
                durationEvent.commit();
            }
        } else {
            // this shouldn't happen, but if it does due to an error on our side, the request processing shouldn't be botched because of it
        }

        return this;
    }

    private void setHttpInfo(AbstractHttpEvent event) {
        event.setTraceId(idProducer.getTraceId());
        event.setSpanId(idProducer.getSpanId());
        event.setHttpMethod(requestInfo.httpMethod());
        event.setUri(requestInfo.uri());
        event.setClient(requestInfo.remoteAddress());
        var ri = resourceInfo;
        if (resourceInfo != null) {
            event.setResourceClass(ri.resourceClass());
            event.setResourceMethod(ri.resourceMethod());
        }
    }
}
