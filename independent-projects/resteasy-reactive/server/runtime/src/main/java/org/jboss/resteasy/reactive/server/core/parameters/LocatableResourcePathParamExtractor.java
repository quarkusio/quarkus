package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;

public class LocatableResourcePathParamExtractor implements ParameterExtractor {

    private final String name;

    public LocatableResourcePathParamExtractor(String name) {
        this.name = name;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        int index = findPathParamIndex(context.getLocatorTarget().getClassPath(), context.getLocatorTarget().getPath());
        if (index >= 0) {
            return context.getLocatorPathParam(index);
        }
        return null;
    }

    private int findPathParamIndex(URITemplate classPathTemplate, URITemplate methodPathTemplate) {
        int index = 0;
        if (classPathTemplate != null) {
            for (URITemplate.TemplateComponent component : classPathTemplate.components) {
                if (component.name != null) {
                    if (component.name.equals(this.name)) {
                        return index;
                    }
                    index++;
                } else if (component.names != null) {
                    for (String nm : component.names) {
                        if (nm.equals(this.name)) {
                            return index;
                        }
                    }
                    index++;
                }
            }
        }
        for (URITemplate.TemplateComponent component : methodPathTemplate.components) {
            if (component.name != null) {
                if (component.name.equals(this.name)) {
                    return index;
                }
                index++;
            } else if (component.names != null) {
                for (String nm : component.names) {
                    if (nm.equals(this.name)) {
                        return index;
                    }
                }
                index++;
            }
        }
        return -1;
    }

}
