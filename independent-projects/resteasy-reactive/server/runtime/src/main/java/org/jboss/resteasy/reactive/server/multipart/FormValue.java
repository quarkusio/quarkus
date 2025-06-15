package org.jboss.resteasy.reactive.server.multipart;

import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Represents one part of a multipart request
 */
public interface FormValue {

    /**
     * @return the simple string value.
     *
     * @throws IllegalStateException
     *         If the body of the part is not a simple string value
     */
    String getValue();

    /**
     * @return The charset of the simple string value
     */
    String getCharset();

    FileItem getFileItem();

    boolean isFileItem();

    /**
     * @return The filename specified in the disposition header.
     */
    String getFileName();

    /**
     * @return The headers that were present in the multipart request, or null if this was not a multipart request
     */
    MultivaluedMap<String, String> getHeaders();
}
