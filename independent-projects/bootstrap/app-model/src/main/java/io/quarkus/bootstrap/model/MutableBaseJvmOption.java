package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public abstract class MutableBaseJvmOption<T extends MutableBaseJvmOption<T>> implements JvmOption, Serializable {

    private static final String EMPTY_STR = "";
    private static final String PROPERTY_VALUE_SEPARATOR = "|";

    private String name;
    private Set<String> values = Set.of();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<String> getValues() {
        return values;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public T addValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("value is blank");
        }
        if (values.isEmpty()) {
            values = new HashSet<>(1);
        }
        for (String v : value.split("\\|")) {
            values.add(v);
        }
        return (T) this;
    }

    protected abstract String getQuarkusExtensionPropertyPrefix();

    @Override
    public void addToQuarkusExtensionProperties(Properties props) {
        props.setProperty(getQuarkusExtensionPropertyPrefix() + name, toPropertyValue());
    }

    protected String toPropertyValue() {
        if (values.isEmpty()) {
            return EMPTY_STR;
        }
        var i = values.iterator();
        if (values.size() == 1) {
            return i.next();
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(i.next());
        while (i.hasNext()) {
            sb.append(PROPERTY_VALUE_SEPARATOR).append(i.next());
        }
        return sb.toString();
    }

    public String toString() {
        return name + "=" + values;
    }
}
