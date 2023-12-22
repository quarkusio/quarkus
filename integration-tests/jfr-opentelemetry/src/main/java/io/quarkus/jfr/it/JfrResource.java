package io.quarkus.jfr.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.jfr.runtime.http.rest.tracing.TracingRestBlockingEvent;
import io.quarkus.jfr.runtime.http.rest.tracing.TracingRestReactiveEndEvent;
import io.quarkus.jfr.runtime.http.rest.tracing.TracingRestReactiveStartEvent;
import io.quarkus.logging.Log;
import jdk.jfr.Configuration;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

@Path("/jfr")
@ApplicationScoped
public class JfrResource {

    final Configuration c = Configuration.create(Paths.get("src/main/resources/quarkus-jfr.jfc"));
    Recording recording;
    java.nio.file.Path dumpFile;
    RecordedEvent blockingEvent;
    RecordedEvent startEvent;
    RecordedEvent endEvent;

    public JfrResource() throws IOException, ParseException {
    }

    @GET
    @Path("/start")
    public void startJfr() {
        recording = new Recording(c);
        recording.start();
    }

    @GET
    @Path("/stop")
    public void stopJfr() throws IOException {
        this.recording.stop();
        dumpFile = Files.createTempFile("dump", "jfr");
        this.recording.dump(dumpFile);
        this.recording.close();
        List<RecordedEvent> recordedEvents = RecordingFile.readAllEvents(dumpFile);
        if (Log.isDebugEnabled()) {
            Log.debug(recordedEvents.size() + " events were recorded");
        }

        for (RecordedEvent e : recordedEvents) {
            if (!e.hasField("requestId") || e.getString("requestId") == null) {
                continue;
            }
            if (Log.isDebugEnabled()) {
                Log.debug(e);
            }

            if (TracingRestBlockingEvent.class.getAnnotation(Name.class).value().equals(e.getEventType().getName())) {
                blockingEvent = e;
            } else if (TracingRestReactiveStartEvent.class.getAnnotation(Name.class).value()
                    .equals(e.getEventType().getName())) {
                startEvent = e;
            } else if (TracingRestReactiveEndEvent.class.getAnnotation(Name.class).value().equals(e.getEventType().getName())) {
                endEvent = e;
            }
        }
    }

    @GET
    @Path("/reset")
    public void reset() {
        blockingEvent = null;
        startEvent = null;
        endEvent = null;
        recording = null;
        dumpFile = null;
    }

    @GET
    @Path("check")
    @Produces(MediaType.APPLICATION_JSON)
    public JfrTracingEventResponse check() {
        return new JfrTracingEventResponse(createBlockEventResponse(), createStartEventResponse(), createEndEventResponse());
    }

    private TracingBlockingEventResponse createBlockEventResponse() {
        if (blockingEvent == null) {
            return null;
        }

        return new TracingBlockingEventResponse(
                blockingEvent.getString("requestId"),
                blockingEvent.getString("traceId"),
                blockingEvent.getString("spanId"),
                blockingEvent.getString("httpMethod"),
                blockingEvent.getString("uri"),
                blockingEvent.getString("resourceClass"),
                blockingEvent.getString("resourceMethod"),
                blockingEvent.getString("client"));
    }

    private TracingStartEventResponse createStartEventResponse() {
        if (startEvent == null) {
            return null;
        }

        return new TracingStartEventResponse(
                startEvent.getString("requestId"),
                startEvent.getString("traceId"),
                startEvent.getString("spanId"),
                startEvent.getString("httpMethod"),
                startEvent.getString("uri"),
                startEvent.getString("resourceClass"),
                startEvent.getString("resourceMethod"),
                startEvent.getString("client"));
    }

    private TracingEndEventResponse createEndEventResponse() {
        if (endEvent == null) {
            return null;
        }

        return new TracingEndEventResponse(
                endEvent.getString("requestId"),
                endEvent.getString("traceId"),
                endEvent.getString("spanId"),
                endEvent.getLong("processDuration"));
    }

    class TracingBlockingEventResponse {

        public String requestId;
        public String traceId;
        public String spanId;
        public String httpMethod;
        public String uri;
        public String resourceClass;
        public String resourceMethod;
        public String client;

        public TracingBlockingEventResponse() {
        }

        public TracingBlockingEventResponse(String requestId, String traceId, String spanId, String httpMethod, String uri,
                String resourceClass, String resourceMethod, String client) {
            this.requestId = requestId;
            this.traceId = traceId;
            this.spanId = spanId;
            this.httpMethod = httpMethod;
            this.uri = uri;
            this.resourceClass = resourceClass;
            this.resourceMethod = resourceMethod;
            this.client = client;
        }
    }

    class TracingStartEventResponse {

        public String requestId;
        public String traceId;
        public String spanId;
        public String httpMethod;
        public String uri;
        public String resourceClass;
        public String resourceMethod;
        public String client;

        public TracingStartEventResponse() {
        }

        public TracingStartEventResponse(String requestId, String traceId, String spanId, String httpMethod, String uri,
                String resourceClass, String resourceMethod, String client) {
            this.requestId = requestId;
            this.traceId = traceId;
            this.spanId = spanId;
            this.httpMethod = httpMethod;
            this.uri = uri;
            this.resourceClass = resourceClass;
            this.resourceMethod = resourceMethod;
            this.client = client;
        }
    }

    class TracingEndEventResponse {

        public String requestId;
        public String traceId;
        public String spanId;
        public long processDuration;

        public TracingEndEventResponse() {
        }

        public TracingEndEventResponse(String requestId, String traceId, String spanId, long processDuration) {
            this.requestId = requestId;
            this.traceId = traceId;
            this.spanId = spanId;
            this.processDuration = processDuration;
        }
    }

    class JfrTracingEventResponse {

        public TracingBlockingEventResponse blocking;
        public TracingStartEventResponse start;
        public TracingEndEventResponse end;

        public JfrTracingEventResponse() {
        }

        public JfrTracingEventResponse(TracingBlockingEventResponse blocking, TracingStartEventResponse start,
                TracingEndEventResponse end) {
            this.blocking = blocking;
            this.start = start;
            this.end = end;
        }
    }
}
