package io.quarkus.qute.deployment.engineconfigurations.section;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;

import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.ResultNode;
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
    public CustomSectionHelper initialize(SectionInitContext context) {
        if (context.getParameter("foo") == null) {
            throw new IllegalStateException("Foo param not found");
        }
        return new CustomSectionHelper();
    }

    class CustomSectionHelper implements SectionHelper {

        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            return CompletableFuture.completedStage(new SingleResultNode(bar, null));
        }
    }

}
