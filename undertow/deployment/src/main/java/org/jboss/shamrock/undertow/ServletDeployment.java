package org.jboss.shamrock.undertow;

import java.util.ArrayList;
import java.util.List;

public class ServletDeployment {

    private final List<ServletData> servlets = new ArrayList<>();

    public void addServlet(ServletData data) {
        servlets.add(data);
    }

    List<ServletData> getServlets() {
        return servlets;
    }
}
