/**
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 * <p>
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.quarkus.templates;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="claprun@redhat.com">Christophe Laprun</a>
 */
public class TemplateRegistry {

    private static final Map<String, QuarkusTemplate> templates = new ConcurrentHashMap<>(7);
    private static final TemplateRegistry INSTANCE = new TemplateRegistry();

    private TemplateRegistry() {
        loadTemplates();
    }

    public static TemplateRegistry getInstance() {
        return INSTANCE;
    }

    public static QuarkusTemplate createTemplateWith(String name) throws NoSuchElementException {
        final QuarkusTemplate template = templates.get(name);
        if (template == null) {
            throw new NoSuchElementException("Unknown template: " + name);
        }

        return template;
    }

    private static void register(QuarkusTemplate template) {
        if (template != null) {
            templates.put(template.getName(), template);
        } else {
            throw new NullPointerException("Cannot register null templates");
        }
    }

    private static void loadTemplates() {
        ServiceLoader<QuarkusTemplate> serviceLoader = ServiceLoader.load(QuarkusTemplate.class);
        serviceLoader.iterator().forEachRemaining(TemplateRegistry::register);
    }
}
