package io.quarkus.rest.client.reactive;

public final class RestClientTestUtil {
    public static String setUrlForClass(Class<?> clazz) {
        return urlPropNameFor(clazz) + "=${test.url}\n";
    }

    private static String urlPropNameFor(Class<?> clazz) {
        return clazz.getName() + "/mp-rest/url";
    }

    private RestClientTestUtil() {
    }
}
