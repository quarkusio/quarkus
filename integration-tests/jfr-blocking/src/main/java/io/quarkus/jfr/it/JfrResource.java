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

import io.quarkus.jfr.runtime.http.rest.RestEndEvent;
import io.quarkus.jfr.runtime.http.rest.RestPeriodEvent;
import io.quarkus.jfr.runtime.http.rest.RestStartEvent;
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
    public JfrRestEventResponse check(@PathParam("name") String name, @PathParam("traceId") String traceId) throws IOException {
        java.nio.file.Path dumpFile = Files.createTempFile("dump", "jfr");
        Recording recording = getRecording(name);
        recording.dump(dumpFile);
        recording.close();

        List<RecordedEvent> recordedEvents = RecordingFile.readAllEvents(dumpFile);
        if (Log.isDebugEnabled()) {
            Log.debug(recordedEvents.size() + " events were recorded");
        }

        RecordedEvent periodEvent = null;
        RecordedEvent startEvent = null;
        RecordedEvent endEvent = null;

        for (RecordedEvent e : recordedEvents) {
            if (Log.isDebugEnabled()) {
                if (e.getEventType().getCategoryNames().contains("Quarkus")) {
                    Log.debug(e);
                }
            }
            if (e.hasField("traceId") && e.getString("traceId").equals(traceId)) {
                if (RestPeriodEvent.class.getAnnotation(Name.class).value().equals(e.getEventType().getName())) {
                    periodEvent = e;
                } else if (RestStartEvent.class.getAnnotation(Name.class).value().equals(e.getEventType().getName())) {
                    startEvent = e;
                } else if (RestEndEvent.class.getAnnotation(Name.class).value().equals(e.getEventType().getName())) {
                    endEvent = e;
                }
            }
        }

        return new JfrRestEventResponse(createRestEvent(periodEvent), createRestEvent(startEvent),
                createRestEvent(endEvent));
    }

    @GET
    @Path("count/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public long count(@PathParam("name") String name) throws IOException {
        java.nio.file.Path dumpFile = Files.createTempFile("dump", "jfr");
        Recording recording = getRecording(name);
        recording.dump(dumpFile);
        recording.close();

        List<RecordedEvent> recordedEvents = RecordingFile.readAllEvents(dumpFile);
        return recordedEvents.stream().filter(r -> r.getEventType().getCategoryNames().contains("quarkus")).count();
    }

    private Recording getRecording(String name) {
        List<Recording> recordings = FlightRecorder.getFlightRecorder().getRecordings();
        Optional<Recording> recording = recordings.stream().filter(r -> r.getName().equals(name)).findFirst();
        return recording.get();
    }

    private RestEvent createRestEvent(RecordedEvent event) {
        if (event == null) {
            return null;
        }
        RestEvent restEvent = new RestEvent();
        setHttpInfo(restEvent, event);

        return restEvent;
    }

    private void setHttpInfo(RestEvent response, RecordedEvent event) {
        response.traceId = event.getString("traceId");
        response.spanId = event.getString("spanId");
        response.httpMethod = event.getString("httpMethod");
        response.uri = event.getString("uri");
        response.resourceClass = event.getString("resourceClass");
        response.resourceMethod = event.getString("resourceMethod");
        response.client = event.getString("client");
    }

    class JfrRestEventResponse {

        public RestEvent period;
        public RestEvent start;
        public RestEvent end;

        public JfrRestEventResponse() {
        }

        public JfrRestEventResponse(RestEvent period, RestEvent start,
                RestEvent end) {
            this.period = period;
            this.start = start;
            this.end = end;
        }
    }

    class RestEvent {

        public String traceId;
        public String spanId;
        public String httpMethod;
        public String uri;
        public String resourceClass;
        public String resourceMethod;
        public String client;
    }
}
