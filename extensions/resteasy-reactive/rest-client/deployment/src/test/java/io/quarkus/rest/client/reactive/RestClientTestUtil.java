package io.quarkus.rest.client.reactive;

public final class RestClientTestUtil {
    public static String setUrlForClass(Class<?> clazz) {
        return urlPropNameFor(clazz) + "=${test.url}\n";
    }

    public static String urlPropNameFor(Class<?> clazz) {
        return propNameFor("url", clazz);
    }

    public static String propNameFor(String property, Class<?> clazz) {
        return clazz.getName() + "/mp-rest/" + property;
    }

    public static String quarkusPropNameFor(String property, Class<?> clazz) {
        return String.format("quarkus.rest-client.\"%s\".%s", clazz.getName(), property);
    }

    private RestClientTestUtil() {
    }
}
