package org.jboss.protean.gizmo;

import java.util.HashMap;
import java.util.Map;

class AnnotationCreatorImpl implements AnnotationCreator {

    private Map<String, Object> values = new HashMap<>();
    private final String annotationType;

    AnnotationCreatorImpl(String annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public void addValue(String name, Object value) {
        values.put(name, value);
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public String getAnnotationType() {
        return annotationType;
    }
}
