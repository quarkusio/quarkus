package io.quarkus.hibernate.accessor;

/** Access requested for a field. Generated writers remove final from ordinary instance fields. */
public enum FieldAccess {
    READ,
    WRITE,
    READ_WRITE
}
