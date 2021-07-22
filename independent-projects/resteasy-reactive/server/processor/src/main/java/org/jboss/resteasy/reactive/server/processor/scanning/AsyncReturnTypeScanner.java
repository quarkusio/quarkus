package org.jboss.resteasy.reactive.server.processor.scanning;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.server.handlers.CompletionStageResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.PublisherResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.UniResponseHandler;
import org.jboss.resteasy.reactive.server.model.FixedHandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.reactivestreams.Publisher;

public class AsyncReturnTypeScanner implements MethodScanner {
    private static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    private static final DotName UNI = DotName.createSimple(Uni.class.getName());
    private static final DotName MULTI = DotName.createSimple(Multi.class.getName());
    private static final DotName PUBLISHER = DotName.createSimple(Publisher.class.getName());

    @Override
    public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext) {
        DotName returnTypeName = method.returnType().name();
        if (returnTypeName.equals(COMPLETION_STAGE)) {
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
