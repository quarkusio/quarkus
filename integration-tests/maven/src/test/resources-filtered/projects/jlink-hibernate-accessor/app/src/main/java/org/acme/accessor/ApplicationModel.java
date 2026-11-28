package org.acme.accessor;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedAccessor;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

@GeneratedAccessor(fields = @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE))
public class ApplicationModel {
    private String value;

    private ApplicationModel(String value) {
        this.value = value;
    }

    private String getValue() {
        return value;
    }

    private void setValue(String value) {
        this.value = value;
    }
}
