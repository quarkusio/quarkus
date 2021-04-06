package io.quarkus.rest.client.reactive;

public final class RestClientTestUtil {
    public static String setUrlForClass(Class<?> clazz) {
        return clazz.getName() + "/mp-rest/url=${test.url}\n";
    }

    private RestClientTestUtil() {
    }
}
