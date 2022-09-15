package org.jboss.resteasy.reactive.common.providers.serialisers;

import jakarta.ws.rs.core.NoContentException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public abstract class PrimitiveBodyHandler {

    public String readFrom(InputStream entityStream, boolean allowEmpty) throws IOException {
        byte[] bytes;
        if (entityStream instanceof ByteArrayInputStream) {
            bytes = new byte[entityStream.available()];
            entityStream.read(bytes);
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024]; //TODO: fix, needs a pure vert.x async read model
            int r;
            while ((r = entityStream.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
            bytes = out.toByteArray();
        }
        if (!allowEmpty) {
            if (bytes.length == 0) {
                throw new NoContentException("No content");
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
