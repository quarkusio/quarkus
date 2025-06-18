package io.quarkus.qute.deployment.typesafe.getters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class TypesafeGettersValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(SomeBean.class, SomeInterface.class)
                    .addAsResource(new StringAsset("""
                            {@io.quarkus.qute.deployment.typesafe.getters.TypesafeGettersValidationTest$SomeBean some}
                            {some.image.length}::{some.hasImage}::{some.hasImage('bar')}::{some.png}::{some.hasPng('bar')}
                            """), "templates/some.html"));

    @Inject
    Template some;

    @Test
    public void testValidation() {
        assertEquals("3::true::true::ping::false", some.data("some", new SomeBean("bar")).render().strip());
    }

    public static class SomeBean implements SomeInterface {

        private final String image;

        SomeBean(String image) {
            this.image = image;
        }

        public String image() {
            return image;
        }

        public boolean hasImage() {
            return image != null;
        }

        public boolean hasImage(String val) {
            return image.equals(val);
        }

    }

    public interface SomeInterface {

        default String png() {
            return "ping";
        }

        default boolean hasPng(String val) {
            return png().equals(val);
        }

    }

}
