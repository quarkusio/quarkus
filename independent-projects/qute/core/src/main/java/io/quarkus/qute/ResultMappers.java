package io.quarkus.qute;

public class ResultMappers {

    public static ResultMapper templateInstanceMapper() {
        return new ResultMapper() {

            @Override
            public boolean appliesTo(TemplateNode.Origin origin, Object result) {
                return result instanceof TemplateInstance;
            }

            @Override
            public String map(Object result, Expression expression) {
                TemplateInstance templateInstance = (TemplateInstance) result;
                return templateInstance.render();
            }
        };
    }
}
