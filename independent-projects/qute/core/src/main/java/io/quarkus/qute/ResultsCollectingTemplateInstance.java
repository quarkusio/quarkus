package io.quarkus.qute;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Collects all rendering results for the specified template instance.
 *
 * @see RenderedResults
 */
public class ResultsCollectingTemplateInstance extends ForwardingTemplateInstance {

    private final TemplateInstance delegate;

    private final BiConsumer<TemplateInstance, String> resultConsumer;

    public ResultsCollectingTemplateInstance(TemplateInstance delegate, BiConsumer<TemplateInstance, String> resultConsumer) {
        this.delegate = delegate;
        this.resultConsumer = resultConsumer;
    }

    @Override
    protected TemplateInstance delegate() {
        return delegate;
    }

    @Override
    public String render() {
        String result = delegate.render();
        resultConsumer.accept(delegate, result);
        return result;
    }

    @Override
    public CompletionStage<String> renderAsync() {
        return delegate.renderAsync().thenApply(r -> {
            resultConsumer.accept(delegate, r);
            return r;
        });
    }

    @Override
    public Multi<String> createMulti() {
        Multi<String> multi = delegate.createMulti();
        StringBuilder builder = new StringBuilder();
        return multi.onItem().invoke(builder::append).onCompletion()
                .invoke(() -> resultConsumer.accept(delegate, builder.toString()));
    }

    @Override
    public Uni<String> createUni() {
        Uni<String> uni = delegate.createUni();
        uni = uni.onItem().invoke(r -> resultConsumer.accept(delegate, r));
        return uni;
    }

    @Override
    public CompletionStage<Void> consume(Consumer<String> consumer) {
        StringBuilder builder = new StringBuilder();
        CompletionStage<Void> cs = delegate.consume(consumer.andThen(builder::append));
        return cs.thenApply(v -> {
            resultConsumer.accept(delegate, builder.toString());
            return v;
        });
    }

}
