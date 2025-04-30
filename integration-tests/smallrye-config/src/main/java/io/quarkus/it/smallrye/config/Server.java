package io.quarkus.it.smallrye.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.Converter;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "http.server")
@RegisterForReflection
public interface Server extends Alias {
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

    @WithConverter(ByteArrayConverter.class)
    byte[] bytes();

    @JsonProperty
    @WithParentName
    Map<String, Form> form();

    @JsonProperty
    Optional<Ssl> ssl();

    @JsonProperty
    Optional<Proxy> proxy();

    @JsonProperty
    Optional<Cors> cors();

    @JsonProperty
    Log log();

    @JsonProperty
    Info info();

    @RegisterForReflection
    interface Form {
        @JsonProperty
        String loginPage();

        @JsonProperty
        String errorPage();

        @JsonProperty
        String landingPage();

        @JsonProperty
        Optional<String> cookie();

        @JsonProperty
        @WithDefault("1")
        List<Integer> positions();
    }

    @RegisterForReflection
    interface Proxy {
        @JsonProperty
        boolean enable();

        @JsonProperty
        int timeout();
    }

    @RegisterForReflection
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

        @JsonProperty
        Period period();

        @JsonProperty
        int days();

        @RegisterForReflection
        enum Pattern {
            COMMON,
            SHORT,
            COMBINED,
            LONG;
        }
    }

    @RegisterForReflection
    interface Cors {
        @JsonProperty
        List<Origin> origins();

        @JsonProperty
        List<String> methods();

        @RegisterForReflection
        interface Origin {
            @JsonProperty
            String host();

            @JsonProperty
            int port();
        }
    }

    @RegisterForReflection
    interface Info {
        @JsonProperty
        Optional<String> name();

        @JsonProperty
        OptionalInt code();

        @JsonProperty
        Optional<List<String>> alias();

        @JsonProperty
        Map<String, List<Admin>> admins();

        @JsonProperty
        Map<String, List<String>> firewall();

        @RegisterForReflection
        interface Admin {
            @JsonProperty
            String username();
        }
    }

    class ByteArrayConverter implements Converter<byte[]> {
        @Override
        public byte[] convert(String value) throws IllegalArgumentException, NullPointerException {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }
}
