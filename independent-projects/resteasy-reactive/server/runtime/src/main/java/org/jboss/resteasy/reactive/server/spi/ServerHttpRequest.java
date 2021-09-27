package org.jboss.resteasy.reactive.server.spi;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;

public interface ServerHttpRequest {

    String getRequestHeader(CharSequence name);

    Iterable<Map.Entry<String, String>> getAllRequestHeaders();

    List<String> getAllRequestHeaders(String name);

    boolean containsRequestHeader(CharSequence accept);

    String getRequestPath();

    String getRequestMethod();

    String getRequestNormalisedPath();

    String getRequestAbsoluteUri();

    String getRequestScheme();

    String getRequestHost();

    void closeConnection();

    String getQueryParam(String name);

    List<String> getAllQueryParams(String name);

    String query();

    Collection<String> queryParamNames();

    boolean isRequestEnded();

    InputStream createInputStream(ByteBuffer existingData);

    InputStream createInputStream();

    ServerHttpResponse pauseRequestInput();

    ServerHttpResponse resumeRequestInput();

    ServerHttpResponse setReadListener(ReadCallback callback);

    /**
     * Unwraps a backing object
     * 
     * @param theType
     * @param <T>
     * @return
     */
    <T> T unwrap(Class<T> theType);

    /**
     * If the underlying transport has handled multipart this can return the result, instead of using resteasy reactives
     * built in parser.
     *
     */
    default FormData getExistingParsedForm() {
        return null;
    }

    interface ReadCallback {

        void done();

        void data(ByteBuffer data);

    }

}
