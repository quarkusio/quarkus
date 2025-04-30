package io.quarkus.hibernate.validator.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

public class ConfigMappingInvalidTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-mappings-validation.properties", "application.properties"))
            .assertException(new Consumer<Throwable>() {
                @Override
                public void accept(final Throwable throwable) {
                    // can't use instanceOf because of different ClassLoaders
                    assertEquals(ConfigValidationException.class.getName(), throwable.getClass().getName());
                    String message = throwable.getMessage();
                    assertTrue(message.contains("validator.server.host must be less than or equal to 3"));
                    assertTrue(message.contains("validator.hierarchy.number must be greater than or equal to 10"));
                    assertTrue(message.contains("validator.repeatable.name size must be between 2"));

                    assertTrue(message.contains("cloud.port must be greater than or equal to 8000"));
                    assertTrue(message.contains("cloud.log.days must be less than or equal to 15"));
                    assertTrue(message.contains("cloud.cors.origins[1].port must be greater than or equal to 8000"));
                    assertTrue(message.contains("cloud.info.name size must be between 0 and 3"));
                    assertTrue(message.contains("cloud.info.code must be less than or equal to 3"));
                    assertTrue(message.contains("cloud.info.alias[0] size must be between 0 and 3"));
                    assertTrue(message.contains("cloud.info.admins.root[1].username size must be between 0 and 4"));
                    assertTrue(message.contains("cloud.info.firewall.accepted[1] size must be between 8 and 15"));
                    assertTrue(message.contains("cloud.proxy.timeout must be less than or equal to 10"));
                    assertTrue(message.contains("cloud server is not prod"));
                }
            });

    @Test
    void invalid() {
        fail();
    }

    @Unremovable
    @ConfigMapping(prefix = "validator.server")
    public interface Server {
        @Max(3)
        String host();

        List<@NotEmpty String> services();
    }

    public interface Parent {
        @Min(10)
        Integer number();
    }

    @Unremovable
    @ConfigMapping(prefix = "validator.hierarchy")
    public interface Child extends Parent {

    }

    @Unremovable
    @ConfigMapping(prefix = "validator.repeatable")
    public interface Repeatable {
        @Size(max = 10)
        @Size(min = 2)
        String name();
    }

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

    @Target({ ElementType.TYPE_USE, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = Prod.Validator.class)
    public @interface Prod {
        String message() default "server is not prod";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        class Validator implements ConstraintValidator<Prod, Cloud> {
            @Override
            public boolean isValid(final Cloud value, final ConstraintValidatorContext context) {
                return value.host().equals("prod");
            }
        }
    }
}
