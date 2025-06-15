package org.jboss.resteasy.reactive.server.multipart;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Used when a Resource method needs to read all the parts of a multipart input without apriori knowledge of the
 * structure of the requests
 */
public interface MultipartFormDataInput {

    Map<String, Collection<FormValue>> getValues();

    class Empty implements MultipartFormDataInput {

        public static final Empty INSTANCE = new Empty();

        @Override
        public Map<String, Collection<FormValue>> getValues() {
            return Collections.emptyMap();
        }
    }
}
