package io.quarkus.it.kogito.drools;

import java.util.ArrayList;
import java.util.Collection;

public class Result {
    private Object value;

    public Result() {
        // empty constructor.
    }

    public Result(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void addValue(Object value) {
        if (!(this.value instanceof Collection)) {
            this.value = new ArrayList<>();
        }
        ((Collection) this.value).add(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Result result = (Result) o;
        return value != null ? value.equals(result.value) : result.value == null;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
