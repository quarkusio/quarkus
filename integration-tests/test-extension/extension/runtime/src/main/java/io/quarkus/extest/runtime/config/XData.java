package io.quarkus.extest.runtime.config;

import java.util.Date;

/**
 * An alternate to XlmData that has no JAXB annotations
 */
public class XData {
    private String name;
    private String model;
    private Date date;

    public String getName() {
        return name;
    }

    public String getModel() {
        return model;
    }

    public Date getDate() {
        return date;
    }
}
