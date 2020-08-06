package io.quarkus.qrs.runtime.core;

public class FormParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;

    public FormParamExtractor(String name, boolean single) {
        this.name = name;
        this.single = single;
    }

    @Override
    public Object extractParameter(QrsRequestContext context) {
        if (single) {
            return context.getContext().request().getFormAttribute(name);
        } else {
            return context.getContext().request().formAttributes().getAll(name);
        }
    }
}
