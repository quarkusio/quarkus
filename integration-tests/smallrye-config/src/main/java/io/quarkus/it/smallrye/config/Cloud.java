package io.quarkus.it.smallrye.config;

import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@Unremovable
@ConfigMapping(prefix = "cloud")
@Prod
public interface Cloud {
    String host();

    @Min(8000)
    int port();

    @WithConverter(DurationConverter.class)
    Duration timeout();

    @WithName("io-threads")
    int threads();

    @WithParentName
    Map<String, Form> form();

    Optional<Ssl> ssl();

    Optional<Proxy> proxy();

    Optional<Cors> cors();

    Log log();

    Info info();

    interface Form {
        String loginPage();

        String errorPage();

        String landingPage();

        Optional<String> cookie();

        @WithDefault("1")
        List<Integer> positions();
    }

    interface Ssl {
        int port();

        String certificate();

        @WithDefault("TLSv1.3,TLSv1.2")
        List<String> protocols();
    }

    interface Proxy {
        boolean enable();

        @Max(10)
        int timeout();
    }

    interface Log {
        @WithDefault("false")
        boolean enabled();

        @WithDefault(".log")
        String suffix();

        @WithDefault("true")
        boolean rotate();

        @WithDefault("COMMON")
        Pattern pattern();

        Period period();

        @Max(15)
        int days();

        enum Pattern {
            COMMON,
            SHORT,
            COMBINED,
            LONG;
        }
    }

    interface Cors {
        List<Origin> origins();

        List<@Size(min = 2) String> methods();

        interface Origin {
            @Size(min = 5)
            String host();

            @Min(8000)
            int port();
        }
    }

    interface Info {
        Optional<@Size(max = 3) String> name();

        @Max(3)
        OptionalInt code();

        Optional<List<@Size(max = 3) String>> alias();

        Map<String, List<Admin>> admins();

        Map<String, List<@Size(min = 8, max = 15) String>> firewall();

        interface Admin {
            @Size(max = 4)
            String username();
        }
    }
}
