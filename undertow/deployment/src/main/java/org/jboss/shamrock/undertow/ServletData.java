package org.jboss.shamrock.undertow;

import java.util.ArrayList;
import java.util.List;

public class ServletData {

    private final String name;
    private final String servletClass;
    private final List<String> mapings = new ArrayList<>();

    public ServletData(String name, String servletClass) {
        this.name = name;
        this.servletClass = servletClass;
    }

    public String getName() {
        return name;
    }

    public String getServletClass() {
        return servletClass;
    }

    public List<String> getMapings() {
        return mapings;
    }
}
