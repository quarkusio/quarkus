package io.quarkus.restclient.config;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "rest-client", phase = ConfigPhase.RUN_TIME)
public class RestClientsConfig {

    /**
     * Configurations of REST client instances.
     *
     * The key can be either the value of the configKey parameter of a `@RegisterRestClient` annotation, or the name of
     * a class bearing that annotation, in which case it is possible to use the short name, as well as fully qualified
     * name.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, RestClientConfig> configs;

    /**
     * By default, REST Client Reactive uses text/plain content type for String values
     * and application/json for everything else.
     *
     * MicroProfile Rest Client spec requires the implementations to always default to application/json.
     * This build item disables the "smart" behavior of RESTEasy Reactive to comply to the spec.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem(defaultValue = "false")
    public Optional<Boolean> disableSmartProduces;

    /**
     * Mode in which the form data are encoded. Possible values are `HTML5`, `RFC1738` and `RFC3986`.
     * The modes are described in the
     * <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/multipart/HttpPostRequestEncoder.EncoderMode.html">Netty
     * documentation</a>
     *
     * By default, Rest Client Reactive uses RFC1738.
     *
     * This property is applicable to reactive REST clients only.
     */
    @ConfigItem
    public Optional<String> multipartPostEncoderMode;

    public Optional<Boolean> getDisableSmartProduces() {
        return disableSmartProduces;
    }

    public Optional<String> getMultipartPostEncoderMode() {
        return multipartPostEncoderMode;
    }
}
