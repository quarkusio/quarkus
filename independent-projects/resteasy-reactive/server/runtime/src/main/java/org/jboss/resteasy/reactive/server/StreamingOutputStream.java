package org.jboss.resteasy.reactive.server;

import java.io.ByteArrayOutputStream;

/**
 * The only reason we use this is to give MessageBodyWriter classes the ability to tell if they are being called in a
 * streaming context
 */
public class StreamingOutputStream extends ByteArrayOutputStream {
}
