package io.quarkus.smallrye.graphql.client.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class GraphQLClientConfig {

    /**
     * The URL location of the target GraphQL service.
     */
    @ConfigItem
    public String url;

    /**
     * HTTP headers to add when communicating with the target GraphQL service.
     * Right now, this only works for the dynamic client.
     */
    @ConfigItem(name = "header")
    public Map<String, String> headers;

}
