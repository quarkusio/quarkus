package io.quarkus.hibernate.accessor.test.other;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class OtherPackageEntity {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private String label;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private long count;

    public OtherPackageEntity() {
    }

    public OtherPackageEntity(String label, long count) {
        this.label = label;
        this.count = count;
    }

    public String getLabel() {
        return label;
    }

    public long getCount() {
        return count;
    }
}
