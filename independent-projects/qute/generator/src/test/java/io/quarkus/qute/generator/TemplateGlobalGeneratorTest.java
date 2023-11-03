package io.quarkus.qute.generator;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.PrimitiveType;
import org.junit.jupiter.api.Test;

import io.quarkus.qute.TemplateGlobal;

public class TemplateGlobalGeneratorTest {

    @Test
    public void testValidation() throws IOException {
        Index index = SimpleGeneratorTest.index(MyGlobals.class);
        ClassInfo myGlobals = index.getClassByName(DotName.createSimple(MyGlobals.class.getName()));

        // Fields
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> TemplateGlobalGenerator.validate(myGlobals.field("privateField")))
                .withMessage(
                        "Global variable field declared on io.quarkus.qute.generator.TemplateGlobalGeneratorTest$MyGlobals must not be private: int io.quarkus.qute.generator.TemplateGlobalGeneratorTest$MyGlobals.privateField");
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> TemplateGlobalGenerator.validate(myGlobals.field("nonStaticField")))
                .withMessage(
                        "Global variable field declared on io.quarkus.qute.generator.TemplateGlobalGeneratorTest$MyGlobals  must be static: int io.quarkus.qute.generator.TemplateGlobalGeneratorTest$MyGlobals.nonStaticField");

        // Methods
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> TemplateGlobalGenerator.validate(myGlobals.method("privateMethod")))
                .withMessage(
                        "Global variable method declared on io.quarkus.qute.generator.TemplateGlobalGeneratorTest$MyGlobals must not be private: int privateMethod()");
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> TemplateGlobalGenerator.validate(myGlobals.method("nonStaticMethod")))
                .withMessage(
                        "Global variable method declared on io.quarkus.qute.generator.TemplateGlobalGeneratorTest$MyGlobals must be static: int nonStaticMethod()");
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> TemplateGlobalGenerator.validate(myGlobals.method("voidMethod")))
                .withMessage(
                        "Global variable method declared on io.quarkus.qute.generator.TemplateGlobalGeneratorTest$MyGlobals must not return void: void voidMethod()");
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> TemplateGlobalGenerator.validate(myGlobals.method("withParamsMethod", PrimitiveType.INT)))
                .withMessage(
                        "Global variable method declared on io.quarkus.qute.generator.TemplateGlobalGeneratorTest$MyGlobals must not accept any parameter: int withParamsMethod(int foo)");
    }

    static class MyGlobals {

        @TemplateGlobal
        private static int privateField;

        @TemplateGlobal
        private int nonStaticField;

        @TemplateGlobal
        private static int privateMethod() {
            return 1;
        }

        @TemplateGlobal
        private int nonStaticMethod() {
            return 1;
        }

        @TemplateGlobal
        private static void voidMethod() {
        }

        @TemplateGlobal
        private static int withParamsMethod(int foo) {
            return foo;
        }

    }

}
