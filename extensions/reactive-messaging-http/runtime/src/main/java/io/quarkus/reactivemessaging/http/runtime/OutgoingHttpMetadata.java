package io.quarkus.reactivemessaging.http.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata for messages sent out by the http connector
 */
public class OutgoingHttpMetadata {
    private final Map<String, List<String>> query;
    private final Map<String, List<String>> headers;
    private final Map<String, String> pathParameters;

    private OutgoingHttpMetadata(Map<String, String> pathParameters, Map<String, List<String>> query,
            Map<String, List<String>> headers) {
        this.pathParameters = pathParameters;
        this.query = query;
        this.headers = headers;
    }

    Map<String, List<String>> getHeaders() {
        return headers;
    }

    Map<String, List<String>> getQuery() {
        return query;
    }

    Map<String, String> getPathParameters() {
        return pathParameters;
    }

    /**
     * OutgoingHttpMetadata buiilder
     */
    public static final class Builder {
        private Map<String, List<String>> query;
        private Map<String, List<String>> headers;
        private Map<String, String> pathParameters;

        /**
         * add a query parameter
         *
         * @param paramName name of the query parameter
         * @param paramValue value of the query parameter
         * @return this
         */
        public Builder addQueryParameter(String paramName, String paramValue) {
            if (query == null) {
                query = new HashMap<>();
            }
            addToMap(query, paramName, paramValue, false);
            return this;
        }

        /**
         * add a query parameter. If there is a previous parameter with the same name, it will be removed
         *
         * @param paramName name of the query parameter
         * @param paramValue value of the query parameter
         * @return this
         */
        public Builder replaceQueryParameter(String paramName, String paramValue) {
            if (query == null) {
                query = new HashMap<>();
            }
            addToMap(query, paramName, paramValue, true);
            return this;
        }

        /**
         * add a HTTP header
         *
         * @param headerName name of the header
         * @param headerValue value of the header
         * @return this
         */
        public Builder addHeader(String headerName, String headerValue) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            addToMap(headers, headerName, headerValue, false);
            return this;
        }

        /**
         * add a HTTP header. If there is a previous header with the same name, it will be removed
         *
         * @param headerName name of the header
         * @param headerValue value of the header
         * @return this
         */
        public Builder replaceHeader(String headerName, String headerValue) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            addToMap(headers, headerName, headerValue, true);
            return this;
        }

        private void addToMap(Map<String, List<String>> map, String headerName, String headerValue, boolean replaceExisting) {
            List<String> values = map.computeIfAbsent(headerName, whatever -> new ArrayList<>());

            if (replaceExisting && !values.isEmpty()) {
                values.clear();
            }

            values.add(headerValue);
        }

        /**
         * Add a path parameter.
         * To use parameters, it is needed to add a placeholder in the URL of the connector in the form of {parameterName}
         *
         * @param parameter path parameter name, should correspond to a placeholder like {parameter} in the URL
         * @param value path parameter value
         * @return this
         */
        public Builder addPathParameter(String parameter, String value) {
            if (pathParameters == null) {
                pathParameters = new HashMap<>();
            }
            pathParameters.put(parameter, value);
            return this;
        }

        /**
         * build the metadata object
         * 
         * @return metadata
         */
        public OutgoingHttpMetadata build() {
            return new OutgoingHttpMetadata(
                    pathParameters == null ? Collections.emptyMap() : pathParameters,
                    query == null ? Collections.emptyMap() : query,
                    headers == null ? Collections.emptyMap() : headers);
        }
    }
}
