package io.quarkus.rest.runtime.util;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class NoContent {

    public static boolean isContentLengthZero(MultivaluedMap httpHeaders) {
        if (httpHeaders == null)
            return false;
        @SuppressWarnings(value = "unchecked")
        String contentLength = (String) httpHeaders.getFirst(HttpHeaders.CONTENT_LENGTH);
        if (contentLength != null) {
            long length = Long.parseLong(contentLength);
            if (length == 0)
                return true;
        }
        return false;
    }
}
