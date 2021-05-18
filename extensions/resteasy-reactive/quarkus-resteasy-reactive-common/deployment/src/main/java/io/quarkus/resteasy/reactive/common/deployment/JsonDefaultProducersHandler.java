package io.quarkus.resteasy.reactive.common.deployment;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COLLECTION;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LIST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MAP;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SET;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.DefaultProducesHandler;

public class JsonDefaultProducersHandler implements DefaultProducesHandler {

    private static final List<MediaType> PRODUCES_APPLICATION_JSON = Collections.singletonList(MediaType.APPLICATION_JSON_TYPE);

    private static final Set<DotName> SUPPORTED_JAVA_TYPES = Set.of(COLLECTION, LIST, SET, MAP);

    @Override
    public List<MediaType> handle(Context context) {
        if (context.config().isDefaultProduces() && isJsonCompatibleType(context)) {
            return PRODUCES_APPLICATION_JSON;
        }
        return Collections.emptyList();
    }

    private boolean isJsonCompatibleType(Context context) {
        Type type = context.nonAsyncReturnType();
        // this doesn't catch every single case, but it should be good enough to cover most common cases
        if ((type.kind() != Type.Kind.CLASS) && (type.kind() != Type.Kind.PARAMETERIZED_TYPE)) {
            return false;
        }
        DotName dotName = type.name();
        if (dotName.toString().startsWith("java")) {
            return SUPPORTED_JAVA_TYPES.contains(dotName);
        }
        if (dotName.toString().startsWith("jakarta")) { // let's be forward compatible
            return false;
        }
        // check if the class is an application class
        return context.index().getClassByName(dotName) != null;
    }
}
