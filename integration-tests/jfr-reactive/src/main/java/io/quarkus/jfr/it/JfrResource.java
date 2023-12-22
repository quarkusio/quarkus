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

import io.quarkus.jfr.runtime.http.rest.RestBlockingEvent;
import io.quarkus.jfr.runtime.http.rest.RestReactiveEndEvent;
import io.quarkus.jfr.runtime.http.rest.RestReactiveStartEvent;
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
            if (RestBlockingEvent.class.getAnnotation(Name.class).value().equals(e.getEventType().getName())) {
                blockingEvent = e;
            } else if (RestReactiveStartEvent.class.getAnnotation(Name.class).value().equals(e.getEventType().getName())) {
                startEvent = e;
            } else if (RestReactiveEndEvent.class.getAnnotation(Name.class).value().equals(e.getEventType().getName())) {
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
    public JfrEventResponse check() {
        return new JfrEventResponse(createBlockEventResponse(), createStartEventResponse(), createEndEventResponse());
    }

    private BlockingEventResponse createBlockEventResponse() {
        if (blockingEvent == null) {
            return null;
        }

        return new BlockingEventResponse(blockingEvent.getString("requestId"),
                blockingEvent.getString("httpMethod"),
                blockingEvent.getString("uri"),
                blockingEvent.getString("resourceClass"),
                blockingEvent.getString("resourceMethod"),
                blockingEvent.getString("client"));
    }

    private StartEventResponse createStartEventResponse() {
        if (startEvent == null) {
            return null;
        }

        return new StartEventResponse(startEvent.getString("requestId"),
                startEvent.getString("httpMethod"),
                startEvent.getString("uri"),
                startEvent.getString("resourceClass"),
                startEvent.getString("resourceMethod"),
                startEvent.getString("client"));
    }

    private EndEventResponse createEndEventResponse() {
        if (endEvent == null) {
            return null;
        }

        return new EndEventResponse(endEvent.getString("requestId"), endEvent.getLong("processDuration"));
    }

    class BlockingEventResponse {

        public String requestId;
        public String httpMethod;
        public String uri;
        public String resourceClass;
        public String resourceMethod;
        public String client;

        public BlockingEventResponse() {
        }

        public BlockingEventResponse(String requestId, String httpMethod, String uri, String resourceClass,
                String resourceMethod, String client) {
            this.requestId = requestId;
            this.httpMethod = httpMethod;
            this.uri = uri;
            this.resourceClass = resourceClass;
            this.resourceMethod = resourceMethod;
            this.client = client;
        }
    }

    class StartEventResponse {

        public String requestId;
        public String httpMethod;
        public String uri;
        public String resourceClass;
        public String resourceMethod;
        public String client;

        public StartEventResponse() {
        }

        public StartEventResponse(String requestId, String httpMethod, String uri, String resourceClass, String resourceMethod,
                String client) {
            this.requestId = requestId;
            this.httpMethod = httpMethod;
            this.uri = uri;
            this.resourceClass = resourceClass;
            this.resourceMethod = resourceMethod;
            this.client = client;
        }
    }

    class EndEventResponse {

        public String requestId;
        public long processDuration;

        public EndEventResponse() {
        }

        public EndEventResponse(String requestId, long processDuration) {
            this.requestId = requestId;
            this.processDuration = processDuration;
        }
    }

    class JfrEventResponse {

        public BlockingEventResponse blocking;
        public StartEventResponse start;
        public EndEventResponse end;

        public JfrEventResponse() {
        }

        public JfrEventResponse(BlockingEventResponse blocking, StartEventResponse start, EndEventResponse end) {
            this.blocking = blocking;
            this.start = start;
            this.end = end;
        }
    }
}
