package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

public class GlobalNamespaceResolverTest {

    @Test
    public void tesNamespaceResolver() {
        Engine engine = Engine.builder()
                .addNamespaceResolver(new NamespaceResolver() {

                    @Override
                    public CompletionStage<Object> resolve(EvalContext context) {
                        if (!context.getName().equals("foo")) {
                            return Results.notFound(context);
                        }
                        CompletableFuture<Object> ret = new CompletableFuture<>();
                        context.evaluate(context.getParams().get(0)).whenComplete((r, e) -> {
                            ret.complete(r);
                        });
                        return ret;
                    }

                    @Override
                    public String getNamespace() {
                        return "global";
                    }
                })
                .build();

        assertEquals("bar", engine.parse("{global:foo('bar')}").render(null));
    }

}
