package io.quarkus.qute;

import io.quarkus.qute.Results.Result;
import io.quarkus.qute.SectionHelperFactory.SectionInitContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Basic {@code with} statement.
 */
public class WithSectionHelper implements SectionHelper {

    private static final String OBJECT = "object";
    private static final String WITH = "with";
    private static final String AS = "as";
    private static final String ALIAS = "alias";

    private final Expression object;
    private final SectionBlock main;
    private final String alias;

    WithSectionHelper(SectionInitContext context) {
        this.object = context.getExpression(OBJECT);
        this.alias = context.getParameter(ALIAS);
        this.main = context.getBlocks().get(0);
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        return context.resolutionContext().evaluate(object)
                .thenCompose(with -> {
                    Object data;
                    if (alias != null) {
                        data = new Mapper() {
                            @Override
                            public Object get(String key) {
                                if (alias.equals(key)) {
                                    return with;
                                }
                                return Result.NOT_FOUND;
                            }
                        };
                    } else {
                        data = with;
                    }
                    ResolutionContext child = context.resolutionContext().createChild(data, null);
                    return context.execute(main, child);
                });
    }

    public static class Factory implements SectionHelperFactory<WithSectionHelper> {

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(WITH);
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder().addParameter(OBJECT).addParameter(AS, Parameter.EMPTY)
                    .addParameter(new Parameter(ALIAS, null, true)).build();
        }

        @Override
        public WithSectionHelper initialize(SectionInitContext context) {
            return new WithSectionHelper(context);
        }

        @Override
        public Map<String, String> initializeBlock(Map<String, String> outerNameTypeInfos, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                String object = block.getParameters().get(OBJECT);
                if (object == null) {
                    throw new IllegalStateException("Object param not present");
                }
                Expression objectExpr = block.addExpression(OBJECT, object);
                if (objectExpr.namespace == null && block.hasParameter(ALIAS)) {
                    // Only validate expressions if alias param is set
                    String alias = block.getParameters().get(ALIAS);
                    Map<String, String> typeInfos = new HashMap<String, String>(outerNameTypeInfos);
                    typeInfos.put(alias, objectExpr.typeCheckInfo);
                    return typeInfos;
                } else {
                    return outerNameTypeInfos;
                }
            } else {
                return Collections.emptyMap();
            }
        }

    }
}
