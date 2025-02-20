package io.quarkus.qute;

import static io.quarkus.qute.EvalSectionHelper.EVAL;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import io.quarkus.qute.EngineBuilder.EngineListener;

/**
 * Evaluates the string representation of the the first parameter as a template, e.g. {@code str:eval('Hello {name}!')}.
 *
 * @see EvalSectionHelper
 */
public class StrEvalNamespaceResolver implements NamespaceResolver, EngineListener {

    private volatile Engine engine;

    private final int priority;

    private final ConcurrentMap<String, Template> templates = new ConcurrentHashMap<>();

    public StrEvalNamespaceResolver() {
        this(-3);
    }

    public StrEvalNamespaceResolver(int priority) {
        this.priority = priority;
    }

    @Override
    public void engineBuilt(Engine engine) {
        this.engine = engine;
    }

    @Override
    public CompletionStage<Object> resolve(EvalContext context) {
        if (!EVAL.equals(context.getName()) || context.getParams().size() != 1) {
            return Results.notFound(context);
        }
        CompletableFuture<Object> ret = new CompletableFuture<>();
        Expression p = context.getParams().get(0);
        if (p.isLiteral()) {
            // We can optimize the case where a literal is used
            String contents = p.getLiteral().toString();
            resolve(ret, context, templates.computeIfAbsent(contents, new Function<String, Template>() {
                @Override
                public Template apply(String contents) {
                    return parse(contents, context.resolutionContext().getTemplate().getVariant().orElse(null));
                }
            }));
        } else {
            context.evaluate(p).whenComplete((r, t) -> {
                if (t != null) {
                    ret.completeExceptionally(t);
                } else {
                    resolve(ret, context,
                            parse(r.toString(), context.resolutionContext().getTemplate().getVariant().orElse(null)));
                }
            });
        }
        return ret;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String getNamespace() {
        return "str";
    }

    private Template parse(String contents, Variant variant) {
        Engine e = engine;
        if (e == null) {
            throw new IllegalStateException("Engine not set");
        }
        return e.parse(contents, variant);
    }

    private void resolve(CompletableFuture<Object> ret, EvalContext context, Template template) {
        template.getRootNode().resolve(context.resolutionContext()).whenComplete((r, t) -> {
            if (t != null) {
                ret.completeExceptionally(t);
            } else {
                StringBuilder sb = new StringBuilder();
                r.process(sb::append);
                ret.complete(new RawString(sb.toString()));
            }
        });
    }

}
