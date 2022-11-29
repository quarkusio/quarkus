package org.jboss.resteasy.reactive.server.core.parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class QueryParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;
    private final boolean encoded;
    private final String separator;

    public QueryParamExtractor(String name, boolean single, boolean encoded, String separator) {
        this.name = name;
        this.single = single;
        this.encoded = encoded;
        this.separator = separator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        Object queryParameter = context.getQueryParameter(name, single, encoded);
        if (separator != null) {
            if (queryParameter instanceof List) { // it's List<String>
                List<String> list = (List<String>) queryParameter;
                List<String> result = new ArrayList<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    String[] parts = list.get(i).split(separator);
                    result.addAll(Arrays.asList(parts));
                }
                queryParameter = result;
            } else if (queryParameter instanceof String) {
                List<String> result = new ArrayList<>(1);
                String[] parts = ((String) queryParameter).split(separator);
                result.addAll(Arrays.asList(parts));
                queryParameter = result;
            } else {
                // can't really happen
            }
        }
        return queryParameter;
    }
}
