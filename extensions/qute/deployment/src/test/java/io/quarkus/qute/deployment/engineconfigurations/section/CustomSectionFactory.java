package io.quarkus.qute.deployment.engineconfigurations.section;

import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;

import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.Expression;
import io.quarkus.qute.ResultNode;
import io.quarkus.qute.Scope;
import io.quarkus.qute.SectionHelper;
import io.quarkus.qute.SectionHelperFactory;
import io.quarkus.qute.SingleResultNode;
import io.quarkus.qute.deployment.engineconfigurations.section.CustomSectionFactory.CustomSectionHelper;

@EngineConfiguration
public class CustomSectionFactory implements SectionHelperFactory<CustomSectionHelper> {

    @Inject
    String bar;

    @Override
    public List<String> getDefaultAliases() {
        return List.of("custom");
    }

    @Override
    public ParametersInfo getParameters() {
        return ParametersInfo.builder().addParameter("foo").build();
    }

    @Override
    public Scope initializeBlock(Scope outerScope, BlockInfo block) {
        block.addExpression("foo", block.getParameter("foo"));
        return outerScope;
    }

    @Override
    public CustomSectionHelper initialize(SectionInitContext context) {
        return new CustomSectionHelper(context.getExpression("foo"));
    }

    class CustomSectionHelper implements SectionHelper {

        private final Expression foo;

        public CustomSectionHelper(Expression foo) {
            this.foo = foo;
        }

        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            return context.evaluate(foo).thenApply(fooVal -> new SingleResultNode(fooVal.toString() + ":" + bar));
        }
    }

}
