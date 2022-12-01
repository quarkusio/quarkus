package io.quarkus.arc.test.annotations.prv;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Supplier;

public class WithPrivateDefault {

    public static final Class<?> PRIVATE_STRING_SUPPLIER = PrivateStringSupplier.class;

    public static final Class<?>[] TO_IMPORT = { WithPrivateDefault.class,
            HasPrivateDefault.class, PrivateStringSupplier.class, PublicStringSupplier.class };

    @Retention(RetentionPolicy.RUNTIME)
    public @interface HasPrivateDefault {
        Class<? extends Supplier<String>> privateDefault() default PrivateStringSupplier.class;

        Class<? extends Supplier<String>>[] privateDefaultArray() default { PrivateStringSupplier.class,
                PublicStringSupplier.class };
    }

    private static class PrivateStringSupplier implements Supplier<String> {

        @Override
        public String get() {
            return "PrivateStringSupplier";
        }
    }

    public static class PublicStringSupplier implements Supplier<String> {

        @Override
        public String get() {
            return "PublicStringSupplier";
        }
    }

}
