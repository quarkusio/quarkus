package org.jboss.resteasy.reactive.server.processor.scanning;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETABLE_FUTURE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETION_STAGE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MULTI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PUBLISHER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.UNI;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.server.handlers.CompletionStageResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.PublisherResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.UniResponseHandler;
import org.jboss.resteasy.reactive.server.model.FixedHandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;

public class AsyncReturnTypeScanner implements MethodScanner {

    @Override
    public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext) {
        DotName returnTypeName = method.returnType().name();
        if (returnTypeName.equals(COMPLETION_STAGE) || returnTypeName.equals(COMPLETABLE_FUTURE)) {
            return Collections.singletonList(new FixedHandlerChainCustomizer(new CompletionStageResponseHandler(),
                    HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE));
        } else if (returnTypeName.equals(UNI)) {
            return Collections.singletonList(new FixedHandlerChainCustomizer(new UniResponseHandler(),
                    HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE));
        }
        if (returnTypeName.equals(MULTI) || returnTypeName.equals(PUBLISHER)) {
            return Collections.singletonList(new FixedHandlerChainCustomizer(new PublisherResponseHandler(),
                    HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE));
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isMethodSignatureAsync(MethodInfo method) {
        DotName returnTypeName = method.returnType().name();
        return returnTypeName.equals(COMPLETION_STAGE) || returnTypeName.equals(UNI) || returnTypeName.equals(MULTI)
                || returnTypeName.equals(PUBLISHER);
    }
}
