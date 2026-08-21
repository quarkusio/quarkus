package io.quarkus.qute.deployment.engineconfigurations.rawcontent;

import java.util.List;
import java.util.concurrent.CompletionStage;

import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.ResultNode;
import io.quarkus.qute.SectionHelper;
import io.quarkus.qute.SectionHelperFactory;
import io.quarkus.qute.SingleResultNode;
import io.quarkus.qute.TextNode;
import io.quarkus.qute.deployment.engineconfigurations.rawcontent.RawContentSectionFactory.RawContentSectionHelper;

@EngineConfiguration
public class RawContentSectionFactory implements SectionHelperFactory<RawContentSectionHelper> {

    @Override
    public List<String> getDefaultAliases() {
        return List.of("raw");
    }

    @Override
    public boolean rawContent() {
        return true;
    }

    @Override
    public RawContentSectionHelper initialize(SectionInitContext context) {
        return new RawContentSectionHelper();
    }

    class RawContentSectionHelper implements SectionHelper {

        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            return context.execute().thenApply(resultNode -> {
                String text = ((TextNode) resultNode).getValue();
                return new SingleResultNode(text.toUpperCase());
            });
        }
    }

}
