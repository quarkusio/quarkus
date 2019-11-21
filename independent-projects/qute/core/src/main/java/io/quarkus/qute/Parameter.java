package io.quarkus.qute;

import io.quarkus.qute.SectionHelperFactory.ParametersInfo;

/**
 * 
 * @see ParametersInfo
 */
public class Parameter {

    public static final String EMPTY = "$empty$";

    public final String name;

    public final String defaultValue;

    public final boolean optional;

    public Parameter(String name, String defaultValue, boolean optional) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.optional = optional;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Parameter [name=").append(name).append(", defaultValue=").append(defaultValue).append(", optional=")
                .append(optional).append("]");
        return builder.toString();
    }

}
