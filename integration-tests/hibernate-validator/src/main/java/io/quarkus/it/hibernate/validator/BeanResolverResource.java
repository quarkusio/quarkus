package io.quarkus.it.hibernate.validator;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.Validator;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.validator.constraints.PasswordStrength;
import org.hibernate.validator.spi.password.PasswordStrengthEstimator;
import org.hibernate.validator.spi.password.PasswordStrengthResult;

import io.quarkus.arc.Unremovable;
import io.quarkus.it.hibernate.validator.injection.MyRuntimeConfiguration;

@Path("/bean-resolver")
public class BeanResolverResource {

    @Inject
    Validator validator;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String validate(@QueryParam("password") String password) {
        return validator.validate(new Password(password)).isEmpty() ? "valid" : "invalid";
    }

    public static class Password {
        @PasswordStrength(min = 1)
        public final String password;

        @PasswordStrength(min = 1, estimator = "bean:strength")
        public final String namedPassword;

        public Password(String password) {
            this.password = password;
            this.namedPassword = password;
        }
    }

    @Singleton
    @Named("strength")
    @Unremovable
    public static class Estimator implements PasswordStrengthEstimator {
        private final String expected;

        @Inject
        public Estimator(MyRuntimeConfiguration configuration) {
            // Access runtime configuration when the service is instantiated, not during static initialization.
            expected = configuration.pattern();
        }

        @Override
        public PasswordStrengthResult estimate(char[] password) {
            return PasswordStrengthResult.simple(new String(password).matches(expected) ? 1 : 0, null);
        }
    }
}
