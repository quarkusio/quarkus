package io.quarkus.it.smallrye.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

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
public interface Server extends Alias {
    @JsonProperty
    String host();

    @JsonProperty
    @Min(8000)
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

    interface Proxy {
        @JsonProperty
        boolean enable();

        @JsonProperty
        @Max(10)
        int timeout();
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

        @JsonProperty
        Period period();

        @JsonProperty
        @Max(15)
        int days();

        @RegisterForReflection
        enum Pattern {
            COMMON,
            SHORT,
            COMBINED,
            LONG;
        }
    }

    interface Cors {
        @JsonProperty
        List<Origin> origins();

        @JsonProperty
        List<@Size(min = 2) String> methods();

        interface Origin {
            @JsonProperty
            @Size(min = 5)
            String host();

            @JsonProperty
            @Min(8000)
            int port();
        }
    }

    interface Info {
        @JsonProperty
        Optional<@Size(max = 3) String> name();

        @JsonProperty
        @Max(3)
        OptionalInt code();

        @JsonProperty
        Optional<List<@Size(max = 3) String>> alias();

        @JsonProperty
        Map<String, List<Admin>> admins();

        @JsonProperty
        Map<String, List<@Size(min = 8, max = 15) String>> firewall();

        interface Admin {
            @JsonProperty
            @Size(max = 4)
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
