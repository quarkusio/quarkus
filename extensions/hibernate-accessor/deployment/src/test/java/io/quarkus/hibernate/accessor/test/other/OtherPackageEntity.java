package io.quarkus.hibernate.accessor.test.other;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class OtherPackageEntity {

    @ReflectionFreeAccessor
    private String label;

    @ReflectionFreeAccessor
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
