package io.quarkus.data.processor;

import static org.hibernate.processor.util.StringUtil.decapitalize;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import org.hibernate.processor.spi.AnnotationMetaEntityContext;

/**
 * Experiment: replace Hibernate processor's lifecycle callbacks before they are registered.
 */
final class LifecycleEventMembers {

    private static final String[] CALLBACKS = {
            "PreInsert", "PostInsert", "PreUpdate", "PostUpdate",
            "PreDelete", "PostDelete", "PreUpsert", "PostUpsert"
    };

    private LifecycleEventMembers() {
    }

    static void add(TypeElement entity, AnnotationMetaEntityContext context) {
        Set<String> names = new HashSet<>();
        for (Element member : context.getAllMembers(entity)) {
            String name = member.getSimpleName().toString();
            names.add(name);
            if (member instanceof ExecutableElement) {
                if (name.startsWith("get") && name.length() > 3) {
                    names.add(decapitalize(name.substring(3)));
                } else if (name.startsWith("is") && name.length() > 2) {
                    names.add(decapitalize(name.substring(2)));
                }
            }
        }
        String entityType = entity.getQualifiedName().toString();
        for (String callback : CALLBACKS) {
            String event = callback + "Event";
            String field = "__quarkusLifecycle" + callback;
            while (names.contains(field) || context.hasMember(field)) {
                field += "_";
            }
            names.add(field);
            String declaration = "@jakarta.inject.Inject private jakarta.enterprise.event.Event<jakarta.data.event."
                    + event + "<" + entityType + ">> " + field + ";\n"
                    + "@jakarta.persistence." + callback + " public void _on" + callback + "(" + entityType
                    + " entity) { if (" + field + " != null) " + field + ".fire(new jakarta.data.event." + event
                    + "<>(entity)); }";
            // Hibernate currently registers its generated callback with putIfAbsent under this internal key.
            context.addMember("event." + event + "." + entityType, declaration);
        }
    }
}
