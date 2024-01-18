package io.quarkus.jfr.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.jfr.runtime.http.rest.RestBlockingEvent;
import io.quarkus.jfr.runtime.http.rest.RestReactiveEndEvent;
import io.quarkus.jfr.runtime.http.rest.RestReactiveStartEvent;
import io.quarkus.logging.Log;
import jdk.jfr.Configuration;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

@Path("/jfr")
@ApplicationScoped
public class JfrResource {

    final Configuration c = Configuration.create(Paths.get("src/main/resources/quarkus-jfr.jfc"));

    public JfrResource() throws IOException, ParseException {
    }

    @GET
    @Path("/start/{name}")
    public void startJfr(@PathParam("name") String name) {
        Recording recording = new Recording(c);
        recording.setName(name);
        recording.start();
    }

    @GET
    @Path("/stop/{name}")
    public void stopJfr(@PathParam("name") String name) throws IOException {
        Recording recording = getRecording(name);
        recording.stop();
    }

    @GET
    @Path("check/{name}/{traceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public JfrEventResponse check(@PathParam("name") String name, @PathParam("traceId") String traceId) throws IOException {
        java.nio.file.Path dumpFile = Files.createTempFile("dump", "jfr");
        Recording recording = getRecording(name);
        recording.dump(dumpFile);
        recording.close();

        List<RecordedEvent> recordedEvents = RecordingFile.readAllEvents(dumpFile);
        if (Log.isDebugEnabled()) {
            Log.debug(recordedEvents.size() + " events were recorded");
        }

        RecordedEvent blockingEvent = null;
        RecordedEvent startEvent = null;
        RecordedEvent endEvent = null;

        for (RecordedEvent e : recordedEvents) {
            if (Log.isDebugEnabled()) {
                if (e.getEventType().getCategoryNames().contains("Quarkus")) {
                    Log.debug(e);
                }
            }
            if (e.hasField("traceId") && e.getString("traceId").equals(traceId)) {
                if (RestBlockingEvent.class.getAnnotation(Name.class).value().equals(e.getEventType().getName())) {
                    blockingEvent = e;
                } else if (RestReactiveStartEvent.class.getAnnotation(Name.class).value().equals(e.getEventType().getName())) {
                    startEvent = e;
                } else if (RestReactiveEndEvent.class.getAnnotation(Name.class).value().equals(e.getEventType().getName())) {
                    endEvent = e;
                }
            }
        }

        return new JfrEventResponse(createBlockEventResponse(blockingEvent), createStartEventResponse(startEvent),
                createEndEventResponse(endEvent));
    }

    private Recording getRecording(String name) {
        List<Recording> recordings = FlightRecorder.getFlightRecorder().getRecordings();
        Optional<Recording> recording = recordings.stream().filter(r -> r.getName().equals(name)).findFirst();
        return recording.get();
    }

    private BlockingEventResponse createBlockEventResponse(RecordedEvent blockingEvent) {
        if (blockingEvent == null) {
            return null;
        }

        return new BlockingEventResponse(
                blockingEvent.getString("traceId"),
                blockingEvent.getString("spanId"),
                blockingEvent.getString("httpMethod"),
                blockingEvent.getString("uri"),
                blockingEvent.getString("resourceClass"),
                blockingEvent.getString("resourceMethod"),
                blockingEvent.getString("client"));
    }

    private StartEventResponse createStartEventResponse(RecordedEvent startEvent) {
        if (startEvent == null) {
            return null;
        }

        return new StartEventResponse(
                startEvent.getString("traceId"),
                startEvent.getString("spanId"),
                startEvent.getString("httpMethod"),
                startEvent.getString("uri"),
                startEvent.getString("resourceClass"),
                startEvent.getString("resourceMethod"),
                startEvent.getString("client"));
    }

    private EndEventResponse createEndEventResponse(RecordedEvent endEvent) {
        if (endEvent == null) {
            return null;
        }

        return new EndEventResponse(
                endEvent.getString("traceId"),
                endEvent.getString("spanId"),
                endEvent.getLong("processDuration"));
    }

    class BlockingEventResponse {

        public String traceId;
        public String spanId;
        public String httpMethod;
        public String uri;
        public String resourceClass;
        public String resourceMethod;
        public String client;

        public BlockingEventResponse() {
        }

        public BlockingEventResponse(String traceId, String spanId, String httpMethod, String uri,
                String resourceClass, String resourceMethod, String client) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.httpMethod = httpMethod;
            this.uri = uri;
            this.resourceClass = resourceClass;
            this.resourceMethod = resourceMethod;
            this.client = client;
        }
    }

    class StartEventResponse {

        public String traceId;
        public String spanId;
        public String httpMethod;
        public String uri;
        public String resourceClass;
        public String resourceMethod;
        public String client;

        public StartEventResponse() {
        }

        public StartEventResponse(String traceId, String spanId, String httpMethod, String uri,
                String resourceClass, String resourceMethod, String client) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.httpMethod = httpMethod;
            this.uri = uri;
            this.resourceClass = resourceClass;
            this.resourceMethod = resourceMethod;
            this.client = client;
        }
    }

    class EndEventResponse {

        public String traceId;
        public String spanId;
        public long processDuration;

        public EndEventResponse() {
        }

        public EndEventResponse(String traceId, String spanId, long processDuration) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.processDuration = processDuration;
        }
    }

    class JfrEventResponse {

        public BlockingEventResponse blocking;
        public StartEventResponse start;
        public EndEventResponse end;

        public JfrEventResponse() {
        }

        public JfrEventResponse(BlockingEventResponse blocking, StartEventResponse start,
                EndEventResponse end) {
            this.blocking = blocking;
            this.start = start;
            this.end = end;
        }
    }
}
