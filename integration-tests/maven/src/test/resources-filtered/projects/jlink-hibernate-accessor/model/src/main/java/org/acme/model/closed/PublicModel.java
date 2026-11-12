package org.acme.model.closed;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedAccessor;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;
import org.acme.model.parent.ParentModel;

@GeneratedAccessor(fields = @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE))
public class PublicModel extends ParentModel {
    private String value;

    @GeneratedFieldAccessor(enabled = false)
    private String fallback = "fallback";

    private PublicModel(String value) {
        this.value = value;
    }

    private String getValue() {
        return value;
    }

    private void setValue(String value) {
        this.value = value;
    }
}
