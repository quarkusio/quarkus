package io.quarkus.it.hibernate.validator.enums;

import jakarta.validation.constraints.NotNull;

/**
 * Simply adding this class to the code, without even referencing it from anywhere,
 * used to make Quarkus startup fail because of an error in Hibernate Validator.
 * See https://github.com/quarkusio/quarkus/issues/3284
 */
public enum MyEnumWithValidatorAnnotation {
    VALUE1,
    VALUE2;

    public static MyEnumWithValidatorAnnotation fromCode(@NotNull String code) {
        return MyEnumWithValidatorAnnotation.valueOf(code);
    }
}
