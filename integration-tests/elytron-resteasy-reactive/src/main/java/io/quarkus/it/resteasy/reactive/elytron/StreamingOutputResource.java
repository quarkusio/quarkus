package io.quarkus.it.resteasy.reactive.elytron;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

import io.smallrye.common.annotation.Blocking;

@Path("/streaming-output-error")
public class StreamingOutputResource {
    private static final int ITEMS_PER_EMIT = 100;

    private static final byte[] CHUNK = "This is one chunk of data.\n".getBytes(StandardCharsets.UTF_8);

    @GET
    @Path("/output")
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public StreamingOutput streamOutput(@QueryParam("fail") @DefaultValue("false") boolean fail) {
        return outputStream -> {
            try {
                writeData(outputStream);
                if (fail) {
                    throw new IOException("dummy failure");
                }
                writeData(outputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void writeData(OutputStream out) throws IOException {
        for (int i = 0; i < ITEMS_PER_EMIT; i++) {
            out.write(CHUNK);
            out.flush();
        }
    }
}
