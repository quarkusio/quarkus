package io.quarkus.qute;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import io.quarkus.qute.EngineBuilder.EngineListener;
import io.quarkus.qute.Template.Fragment;

/**
 * Renders a matching fragment from the current template.
 *
 * @see FragmentSectionHelper
 */
public class FragmentNamespaceResolver implements NamespaceResolver, EngineListener {

    public static final String FRG = "frg";
    public static final String FRAGMENT = "fragment";
    public static final String CAP = "cap";
    public static final String CAPTURE = "capture";

    private final String namespace;

    private final int priority;

    private volatile Engine engine;

    public FragmentNamespaceResolver() {
        this(FRG, -1);
    }

    public FragmentNamespaceResolver(String namespace) {
        this(namespace, -1);
    }

    public FragmentNamespaceResolver(String namespace, int priority) {
        this.namespace = namespace;
        this.priority = priority;
    }

    @Override
    public void engineBuilt(Engine engine) {
        this.engine = engine;
    }

    @Override
    public CompletionStage<Object> resolve(EvalContext context) {
        String id = context.getName();
        Template template = null;
        int idx = id.lastIndexOf('$');
        if (idx != -1) {
            // the part before the last occurence of a dollar sign is the template identifier
            String templateId = id.substring(0, idx);
            Engine e = engine;
            if (e == null) {
                throw new TemplateException("Engine not set");
            }
            template = e.getTemplate(templateId);
            if (template == null) {
                throw new TemplateException("Template not found: " + templateId);
            }
            // the part after the last occurence of a dollar sign is the fragment identifier
            id = id.substring(idx + 1);
        } else {
            template = context.resolutionContext().getTemplate();
        }
        Fragment fragment = template.getFragment(id);
        if (fragment != null) {
            CompletableFuture<Object> ret = new CompletableFuture<>();
            if (!context.getParams().isEmpty()) {
                EvaluatedParams params = EvaluatedParams.evaluate(context);
                params.stage.whenComplete((r, t) -> {
                    if (t != null) {
                        ret.completeExceptionally(t);
                    } else {
                        Map<String, Object> args = new HashMap<>();
                        for (int i = 0; i < context.getParams().size(); i++) {
                            try {
                                Object result = params.getResult(i);
                                if (result instanceof NamedArgument arg) {
                                    args.put(arg.getName(), arg.getValue());
                                } else {
                                    ret.completeExceptionally(
                                            new TemplateException("Named argument expected: " + result.getClass()));
                                    break;
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                ret.completeExceptionally(e);
                            }
                        }
                        ResolutionContext child = context.resolutionContext().createChild(Mapper.wrap(args), null);
                        fragment.getRootNode().resolve(child, Map.of(Template.Fragment.ATTRIBUTE, true))
                                .whenComplete((r2, t2) -> {
                                    if (t2 != null) {
                                        ret.completeExceptionally(t2);
                                    } else {
                                        StringBuilder sb = new StringBuilder();
                                        r2.process(sb::append);
                                        ret.complete(sb.toString());
                                    }
                                });
                    }
                });
            } else {
                fragment.getRootNode().resolve(context.resolutionContext(), Map.of(Template.Fragment.ATTRIBUTE, true))
                        .whenComplete((r, t) -> {
                            if (t != null) {
                                ret.completeExceptionally(t);
                            } else {
                                StringBuilder sb = new StringBuilder();
                                r.process(sb::append);
                                ret.complete(sb.toString());
                            }
                        });
            }
            return ret;
        }
        return Results.notFound(context);
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

}
