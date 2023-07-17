package io.quarkus.restclient.config;

import org.eclipse.microprofile.config.spi.Converter;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;

public class QueryParamStyleConverter implements Converter<QueryParamStyle> {
    @Override
    public QueryParamStyle convert(String value) throws IllegalArgumentException, NullPointerException {
        return QueryParamStyle.valueOf(value);
    }
}
