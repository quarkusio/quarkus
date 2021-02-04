package io.quarkus.it.smallrye.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "server")
public interface Server {
    @JsonProperty
    String host();

    @JsonProperty
    int port();

    @JsonProperty
    @WithConverter(DurationConverter.class)
    Duration timeout();

    @JsonProperty
    @WithName("io-threads")
    int threads();

    @JsonProperty
    @WithParentName
    Map<String, Form> form();

    @JsonProperty
    Optional<Ssl> ssl();

    @JsonProperty
    Optional<Proxy> proxy();

    @JsonProperty
    Log log();

    interface Form {
        @JsonProperty
        String loginPage();

        @JsonProperty
        String errorPage();

        @JsonProperty
        String landingPage();

        @JsonProperty
        Optional<String> cookie();
    }

    interface Ssl {
        @JsonProperty
        int port();

        @JsonProperty
        String certificate();

        @JsonProperty
        @WithDefault("TLSv1.3,TLSv1.2")
        List<String> protocols();
    }

    interface Proxy {
        @JsonProperty
        boolean enable();
    }

    interface Log {
        @JsonProperty
        @WithDefault("false")
        boolean enabled();

        @JsonProperty
        @WithDefault(".log")
        String suffix();

        @JsonProperty
        @WithDefault("true")
        boolean rotate();

        @JsonProperty
        @WithDefault("COMMON")
        Pattern pattern();

        @RegisterForReflection
        enum Pattern {
            COMMON,
            SHORT,
            COMBINED,
            LONG;
        }
    }
}
