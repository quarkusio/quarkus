package io.quarkus.qute;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VirtualMethodParametersTest {

    @Test
    public void testVirtualMethodParameters() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ValueResolver() {

            @Override
            public CompletionStage<Object> resolve(EvalContext context) {
                if (context.getName().equals("item") || context.getName().equals("foo")) {
                    return CompletedStage.of(true);
                }
                if (context.getName().equals("name") && Boolean.parseBoolean(context.getBase().toString())) {
                    return CompletedStage.of(4);
                }
                if (context.getName().equals("ping") && Boolean.parseBoolean(context.getBase().toString())
                        && context.getParams().size() == 1) {
                    return CompletedStage.of(3);
                }
                if (context.getName().equals("call") && Boolean.parseBoolean(context.getBase().toString())
                        && context.getParams().size() == 2) {
                    CompletableFuture<Object> ret = new CompletableFuture<>();
                    context.evaluate(context.getParams().get(0)).thenCombine(context.evaluate(context.getParams().get(1)),
                            (r1, r2) -> ret.complete(new BigDecimal(r1.toString()).add(new BigDecimal(r2.toString()))));
                    return ret;
                }
                return null;
            }
        }).build();
        Assertions.assertEquals("7", engine.parse("{foo.call(item.name, item.ping(1))}").render());
    }

}
