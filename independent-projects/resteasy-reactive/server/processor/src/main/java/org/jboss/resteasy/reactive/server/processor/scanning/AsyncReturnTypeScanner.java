package org.jboss.resteasy.reactive.server.processor.scanning;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.server.handlers.CompletionStageResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.MultiResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.UniResponseHandler;
import org.jboss.resteasy.reactive.server.model.FixedHandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;

public class AsyncReturnTypeScanner implements MethodScanner {
    private static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    private static final DotName UNI = DotName.createSimple(Uni.class.getName());
    private static final DotName MULTI = DotName.createSimple(Multi.class.getName());

    @Override
    public List<HandlerChainCustomizer> scan(MethodInfo method, Map<String, Object> methodContext) {
        if (method.returnType().name().equals(COMPLETION_STAGE)) {
            return Collections.singletonList(new FixedHandlerChainCustomizer(new CompletionStageResponseHandler(),
                    HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE));
        } else if (method.returnType().name().equals(UNI)) {
            return Collections.singletonList(new FixedHandlerChainCustomizer(new UniResponseHandler(),
                    HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE));
        }
        if (method.returnType().name().equals(MULTI)) {
            return Collections.singletonList(new FixedHandlerChainCustomizer(new MultiResponseHandler(),
                    HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE));
        }
        return Collections.emptyList();
    }
}
