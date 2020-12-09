package io.quarkus.arc.deployment;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Make it possible to register one or more custom CDI contexts.
 * If you are registering a new context, you also pass in the respective annotation value into the constructor either in
 * {@link DotName} form, or as {@code Class<? extends Annotation>}.
 *
 * This information is then leveraged in {@link CustomScopeAnnotationsBuildItem} which allows consumers to browse
 * all known custom scoped within deployment even early in the build process.
 * 
 * This build item will be removed at some point post Quarkus 1.11.
 * 
 * @deprecated User {@link ContextRegistrationPhaseBuildItem} instead
 */
@Deprecated
public final class ContextRegistrarBuildItem extends MultiBuildItem {

    private static final Logger LOGGER = Logger.getLogger(ContextRegistrarBuildItem.class);

    private final ContextRegistrar contextRegistrar;
    private final Collection<DotName> annotationNames;

    public ContextRegistrarBuildItem(ContextRegistrar contextRegistrar, DotName... annotationsNames) {
        this.contextRegistrar = contextRegistrar;
        if (annotationsNames.length == 0) {
            // log info level - usually you want to pass in annotation name as well
            LOGGER.infof("A ContextRegistrarBuildItem was created but no annotation name/class was specified." +
                    "This information can be later on consumed by other extensions via CustomScopeAnnotationsBuildItem, " +
                    "please consider adding it.");
        }
        Collection<DotName> names = new ArrayList<>(annotationsNames.length);
        for (DotName name : annotationsNames) {
            names.add(name);
        }
        this.annotationNames = names;
    }

    public ContextRegistrarBuildItem(ContextRegistrar contextRegistrar, Class<? extends Annotation>... annotationsClasses) {
        this(contextRegistrar, Arrays.stream(annotationsClasses).map(Class::getName)
                .map(DotName::createSimple).toArray(DotName[]::new));
    }

    public ContextRegistrar getContextRegistrar() {
        return contextRegistrar;
    }

    public Collection<DotName> getAnnotationNames() {
        return annotationNames;
    }
}
