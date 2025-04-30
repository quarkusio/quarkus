package io.quarkus.qute;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public abstract class ForwardingTemplateInstance implements TemplateInstance {

    protected abstract TemplateInstance delegate();

    @Override
    public TemplateInstance data(Object data) {
        delegate().data(data);
        return this;
    }

    @Override
    public TemplateInstance data(String key, Object data) {
        delegate().data(key, data);
        return this;
    }

    @Override
    public TemplateInstance setAttribute(String key, Object value) {
        delegate().setAttribute(key, value);
        return this;
    }

    @Override
    public Object getAttribute(String key) {
        return delegate().getAttribute(key);
    }

    @Override
    public String render() {
        return delegate().render();
    }

    @Override
    public CompletionStage<String> renderAsync() {
        return delegate().renderAsync();
    }

    @Override
    public Multi<String> createMulti() {
        return delegate().createMulti();
    }

    @Override
    public Uni<String> createUni() {
        return delegate().createUni();
    }

    @Override
    public CompletionStage<Void> consume(Consumer<String> consumer) {
        return delegate().consume(consumer);
    }

    @Override
    public long getTimeout() {
        return delegate().getTimeout();
    }

    @Override
    public Template getTemplate() {
        return delegate().getTemplate();
    }

    @Override
    public Template getFragment(String id) {
        return delegate().getFragment(id);
    }

    @Override
    public TemplateInstance onRendered(Runnable action) {
        delegate().onRendered(action);
        return this;
    }

}
