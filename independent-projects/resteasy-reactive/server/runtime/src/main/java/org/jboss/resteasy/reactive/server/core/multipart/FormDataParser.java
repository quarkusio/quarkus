package org.jboss.resteasy.reactive.server.core.multipart;

import java.io.Closeable;
import java.io.IOException;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

/**
 * Parser for form data. This can be used by down-stream handlers to parse
 * form data.
 * <p>
 * This parser must be closed to make sure any temporary files have been cleaned up.
 *
 * @author Stuart Douglas
 */
public interface FormDataParser extends Closeable {

    /**
     * Parse the form data asynchronously. If all the data cannot be read immediately then a read listener will be
     * registered, and the data will be parsed by the read thread.
     * <p>
     * The method can either invoke the next handler directly, or may delegate to the IO thread
     * to perform the parsing.
     */
    void parse() throws Exception;

    /**
     * Parse the data, blocking the current thread until parsing is complete. For blocking handlers this method is
     * more efficient than {@link #parse(ResteasyReactiveRequestContext next)}, as the calling thread should do that
     * actual parsing, rather than the read thread
     *
     * @return The parsed form data
     * @throws IOException If the data could not be read
     */
    FormData parseBlocking() throws IOException;

    /**
     * Closes the parser, and removes and temporary files that may have been created.
     *
     * @throws IOException
     */
    void close() throws IOException;

    /**
     * Sets the character encoding that will be used by this parser. If the request is already processed this will have
     * no effect
     *
     * @param encoding The encoding
     */
    void setCharacterEncoding(String encoding);
}
