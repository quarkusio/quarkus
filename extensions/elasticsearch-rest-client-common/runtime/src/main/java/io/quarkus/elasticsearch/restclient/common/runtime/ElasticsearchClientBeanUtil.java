package io.quarkus.elasticsearch.restclient.common.runtime;

import java.util.Locale;

public final class ElasticsearchClientBeanUtil {

    private ElasticsearchClientBeanUtil() {
    }

    public static final String DEFAULT_ELASTICSEARCH_CLIENT_NAME = "<default>";

    public static boolean isDefault(String clientName) {
        return DEFAULT_ELASTICSEARCH_CLIENT_NAME.equals(clientName);
    }

    public static String activeKey(String client) {
        return String.format(
                Locale.ROOT, "quarkus.elasticsearch.%sactive",
                isDefault(client) ? "" : "\"" + client + "\".");
    }

    public static String activeKey(String radical, String client) {
        return String.format(
                Locale.ROOT, "%s%sactive",
                radical,
                isDefault(client) ? "" : "\"" + client + "\".");
    }
}
