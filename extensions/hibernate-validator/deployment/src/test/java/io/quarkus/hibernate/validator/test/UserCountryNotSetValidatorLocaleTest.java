package io.quarkus.hibernate.validator.test;

import jakarta.inject.Inject;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class UserCountryNotSetValidatorLocaleTest {

    @Inject
    ValidatorFactory validatorFactory;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class).addClasses(Bean.class)
            .addAsResource(new StringAsset("foo=bar"), "application.properties"))
            .setBeforeAllCustomizer(new Runnable() {
                @Override
                public void run() {
                    userCountry = System.clearProperty("user.country");
                    userLanguage = System.clearProperty("user.language");
                }
            }).setAfterAllCustomizer(new Runnable() {
                @Override
                public void run() {
                    if (userCountry != null) {
                        System.setProperty("user.country", userCountry);
                    }
                    System.setProperty("user.language", userLanguage);
                }
            });

    private static String userCountry;
    private static String userLanguage;

    @Test
    public void testApplicationStarts() {
        // we don't really need to test anything, just make sure that the application starts
    }

    static class Bean {

        public Bean(String name) {
            super();
            this.name = name;
        }

        @Pattern(regexp = "A.*", message = "{pattern.message}")
        private String name;
    }
}
