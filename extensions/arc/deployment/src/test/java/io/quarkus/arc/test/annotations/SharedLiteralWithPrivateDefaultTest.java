package io.quarkus.arc.test.annotations;

import java.util.function.Supplier;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.annotations.prv.WithPrivateDefault;
import io.quarkus.test.QuarkusUnitTest;

public class SharedLiteralWithPrivateDefaultTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WithPrivateDefault.TO_IMPORT)
                    .addClasses(PrivateDefaultBean.class, MySupplier.class, OtherSupplier.class));

    @Inject
    PrivateDefaultBean privateDefaultBean;

    private WithPrivateDefault.HasPrivateDefault findAnnotation(InjectedField field) {
        return (WithPrivateDefault.HasPrivateDefault) field.injectionPoint.getAnnotated().getAnnotations()
                .stream().filter(ann -> ann.annotationType().equals(WithPrivateDefault.HasPrivateDefault.class))
                .findFirst().orElseThrow(() -> new IllegalStateException("Didn't found HasPrivateDefault annotation"));
    }

    @Test
    public void testLoadBeanAnnotatedWithPrivateDefault() {
        WithPrivateDefault.HasPrivateDefault defaultAnnotation = findAnnotation(privateDefaultBean.usingDefault);
        Assertions.assertEquals(WithPrivateDefault.PRIVATE_STRING_SUPPLIER, defaultAnnotation.privateDefault());
        Assertions.assertArrayEquals(
                new Class<?>[] { WithPrivateDefault.PRIVATE_STRING_SUPPLIER, WithPrivateDefault.PublicStringSupplier.class },
                defaultAnnotation.privateDefaultArray());

        WithPrivateDefault.HasPrivateDefault overwriteAnnotation = findAnnotation(privateDefaultBean.overwriteDefault);
        Assertions.assertEquals(MySupplier.class, overwriteAnnotation.privateDefault());
        Assertions.assertArrayEquals(new Class<?>[] { MySupplier.class, OtherSupplier.class },
                overwriteAnnotation.privateDefaultArray());
    }

    @Dependent
    public static class PrivateDefaultBean {

        @WithPrivateDefault.HasPrivateDefault
        @Inject
        InjectedField usingDefault;

        @WithPrivateDefault.HasPrivateDefault(privateDefault = MySupplier.class, privateDefaultArray = { MySupplier.class,
                OtherSupplier.class })
        @Inject
        InjectedField overwriteDefault;
    }

    public static class MySupplier implements Supplier<String> {

        @Override
        public String get() {
            return "MySupplier";
        }
    }

    public static class OtherSupplier implements Supplier<String> {

        @Override
        public String get() {
            return "OtherSupplier";
        }
    }

    @Dependent
    public static class InjectedField {
        @Inject
        InjectionPoint injectionPoint;
    }

}
