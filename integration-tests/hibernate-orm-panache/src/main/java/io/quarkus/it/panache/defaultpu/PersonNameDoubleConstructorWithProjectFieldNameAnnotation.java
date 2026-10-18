package io.quarkus.it.panache.defaultpu;

import io.quarkus.hibernate.orm.panache.common.ProjectedFieldName;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PersonNameDoubleConstructorWithProjectFieldNameAnnotation extends PersonName {
    @SuppressWarnings("unused")
    public PersonNameDoubleConstructorWithProjectFieldNameAnnotation(String uniqueName, String name, Object fakeParameter) {
        super(uniqueName, name);
    }

    @SuppressWarnings("unused")
    public PersonNameDoubleConstructorWithProjectFieldNameAnnotation(@ProjectedFieldName("uniqueName") String uniqueName,
            String name) {
        super(uniqueName, name);
    }
}
