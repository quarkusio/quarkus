package io.quarkus.restclient.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.FallbackConfigSourceInterceptor;

public class RestClientFallbackConfigSourceInterceptor extends FallbackConfigSourceInterceptor {

    private static final String QUARKUS_CONFIG_PREFIX = "quarkus.rest-client.";
    private static final String MP_REST = "/mp-rest/";
    private static final String MAX_REDIRECTS = "max-redirects";
    private static final String MAX_REDIRECTS_LEGACY_PROPERTY = "quarkus.rest.client.max-redirects";
    private static final Map<String, String> CLIENT_PROPERTIES;
    private static final Map<String, String> CLIENT_PROPERTIES_INVERSE;
    private static final Map<String, String> GLOBAL_PROPERTIES;
    private static final Map<String, String> GLOBAL_PROPERTIES_INVERSE;
    private static final Function<String, String> MAPPING_FUNCTION = new MappingFunction();

    static {
        CLIENT_PROPERTIES = new HashMap<>();
        CLIENT_PROPERTIES.put("url", "url");
        CLIENT_PROPERTIES.put("uri", "uri");
        CLIENT_PROPERTIES.put("scope", "scope");
        CLIENT_PROPERTIES.put("providers", "providers");
        CLIENT_PROPERTIES.put("connect-timeout", "connectTimeout");
        CLIENT_PROPERTIES.put("read-timeout", "readTimeout");
        CLIENT_PROPERTIES.put("hostname-verifier", "hostnameVerifier");
        CLIENT_PROPERTIES.put("trust-store", "trustStore");
        CLIENT_PROPERTIES.put("trust-store-password", "trustStorePassword");
        CLIENT_PROPERTIES.put("trust-store-type", "trustStoreType");
        CLIENT_PROPERTIES.put("key-store", "keyStore");
        CLIENT_PROPERTIES.put("key-store-password", "keyStorePassword");
        CLIENT_PROPERTIES.put("key-store-type", "keyStoreType");
        CLIENT_PROPERTIES.put("follow-redirects", "followRedirects");
        CLIENT_PROPERTIES.put("proxy-address", "proxyAddress");
        CLIENT_PROPERTIES.put("query-param-style", "queryParamStyle");

        CLIENT_PROPERTIES_INVERSE = inverseMap(CLIENT_PROPERTIES);

        GLOBAL_PROPERTIES = new HashMap<>();
        GLOBAL_PROPERTIES.put("quarkus.rest-client.multipart-post-encoder-mode",
                "quarkus.rest.client.multipart-post-encoder-mode");
        GLOBAL_PROPERTIES.put("quarkus.rest-client.disable-smart-produces",
                "quarkus.rest-client-reactive.disable-smart-produces");

        GLOBAL_PROPERTIES_INVERSE = inverseMap(GLOBAL_PROPERTIES);
    }

    public RestClientFallbackConfigSourceInterceptor() {
        super(MAPPING_FUNCTION);
    }

    /**
     * If an MP-style property is detected (e.g. "prefix/mp-rest/url"),
     * we need to include the relevant Quarkus-style property name ("quarkus.rest-client.prefix.url") in the iteration.
     *
     * This is required so that the BuildTimeConfigurationReader is aware that it should create the configuration objects for
     * REST clients ({@link RestClientConfig}).
     */
    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        final Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            final String name = namesIterator.next();
            names.add(name);

            String[] prefixAndProperty = extractMPClientPrefixAndProperty(name);
            if (prefixAndProperty != null) { // effectively if name.contains("/mp-rest/")
                String clientPrefix = prefixAndProperty[0];
                String mpPropertyName = prefixAndProperty[1];
                String quarkusPropertyName = CLIENT_PROPERTIES_INVERSE.get(mpPropertyName);
                if (quarkusPropertyName != null) {
                    names.add(QUARKUS_CONFIG_PREFIX + clientPrefix + "." + quarkusPropertyName);
                }
            } else if (GLOBAL_PROPERTIES_INVERSE.containsKey(name)) {
                names.add(GLOBAL_PROPERTIES_INVERSE.get(name));
            }
        }
        return names.iterator();
    }

    /**
     * Splits a property key into client prefix and property name. If given key doesn't contain a client prefix, null will be
     * returned in the first array item.
     *
     * Examples:
     * <li>`client-prefix.url` will return `String[] {"client-prefix", "url"}`</li>
     * <li>`"client.prefix".url` will return `String[] {"client.prefix", "url"}`</li>
     * <li>`"disable-smart-produces` will return `String[] {null, "disable-smart-produces"}`</li>
     *
     * @param key property key
     * @return two-item array containing the client prefix and the property name
     */
    static String[] extractQuarkusClientPrefixAndProperty(String key) {
        for (int i = 0; i < key.length(); i++) {
            if (key.charAt(i) == '"') { // opening quote -> increase index until closing quote is found
                do {
                    i++;
                } while (i < key.length() && key.charAt(i) != '"');
            }
            if (i < key.length() && key.charAt(i) == '.') { // the first dot character is taken as a delimiter
                return new String[] { key.substring(0, i), key.substring(i + 1) };
            }
        }
        return new String[] { null, key };
    }

    static String[] extractMPClientPrefixAndProperty(String key) {
        int delimiterIdx = key.indexOf(MP_REST);
        if (delimiterIdx > 0) {
            String prefix = key.substring(0, delimiterIdx);
            if (prefix.contains(".")) {
                prefix = '"' + prefix + '"';
            }
            String property = key.substring(delimiterIdx + MP_REST.length());
            return new String[] { prefix, property };
        }
        return null;
    }

    private static Map<String, String> inverseMap(Map<String, String> map) {
        HashMap<String, String> inverse = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            inverse.put(entry.getValue(), entry.getKey());
        }
        return inverse;
    }

    private static class MappingFunction implements Function<String, String> {
        @Override
        public String apply(String key) {
            if (key.startsWith(QUARKUS_CONFIG_PREFIX)) {
                String remainder = key.substring(QUARKUS_CONFIG_PREFIX.length()); // strip "quarkus.rest-client."
                String[] prefixAndProperty = extractQuarkusClientPrefixAndProperty(remainder);
                if (prefixAndProperty[0] != null && CLIENT_PROPERTIES.containsKey(prefixAndProperty[1])) { // client property
                    String clientPrefix = prefixAndProperty[0];
                    if (clientPrefix.startsWith("\"") && clientPrefix.endsWith("\"")) {
                        clientPrefix = clientPrefix.substring(1, clientPrefix.length() - 1);
                    }
                    return clientPrefix + "/mp-rest/" + CLIENT_PROPERTIES.get(prefixAndProperty[1]);
                }
                if (prefixAndProperty[0] == null && GLOBAL_PROPERTIES.containsKey(key)) { // global property
                    return GLOBAL_PROPERTIES.get(key);
                }
                // special cases
                if (prefixAndProperty[0] != null && MAX_REDIRECTS.equals(prefixAndProperty[1])) {
                    return MAX_REDIRECTS_LEGACY_PROPERTY;
                }
            }
            return key;
        }
    }
}
