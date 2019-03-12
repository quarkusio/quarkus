/**
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 * <p>
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.quarkus.templates;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.QuarkusTemplate;
import io.quarkus.templates.rest.BasicRest;

/**
 * @author <a href="claprun@redhat.com">Christophe Laprun</a>
 */
public class TemplateRegistry {

    private static final Map<String, QuarkusTemplate> templates = new ConcurrentHashMap<>(7);

    public static QuarkusTemplate createTemplateWith(String name) throws IllegalArgumentException {
        final QuarkusTemplate template = templates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("Unknown template: " + name);
        }

        return template;
    }

    static {
        // todo: switch to ServiceLoader
        registerTemplate(new BasicRest());
    }

    public static boolean registerTemplate(QuarkusTemplate template) {
        if (template == null) {
            return false;
        }

        return templates.put(template.getName(), template) == null;
    }
}
