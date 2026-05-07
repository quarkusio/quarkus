package io.quarkus.elasticsearch.restclient.common.runtime;

public final class ElasticsearchClientBeanUtil {

    private ElasticsearchClientBeanUtil() {
    }

    public static final String DEFAULT_ELASTICSEARCH_CLIENT_NAME = "<default>";

    public static boolean isDefault(String clientName) {
        return DEFAULT_ELASTICSEARCH_CLIENT_NAME.equals(clientName);
    }

}
