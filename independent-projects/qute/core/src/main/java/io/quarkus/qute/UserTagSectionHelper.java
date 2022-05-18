package io.quarkus.qute;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class UserTagSectionHelper extends IncludeSectionHelper implements SectionHelper {

    private static final String NESTED_CONTENT = "nested-content";

    protected final boolean isNestedContentNeeded;

    UserTagSectionHelper(Supplier<Template> templateSupplier, Map<String, SectionBlock> extendingBlocks,
            Map<String, Expression> parameters, boolean isIsolated, boolean isNestedContentNeeded) {
        super(templateSupplier, extendingBlocks, parameters, isIsolated);
        this.isNestedContentNeeded = isNestedContentNeeded;
    }

    @Override
    protected boolean optimizeIfNoParams() {
        return false;
    }

    @Override
    protected void addAdditionalEvaluatedParams(SectionResolutionContext context, Map<String, Object> evaluatedParams) {
        if (isNestedContentNeeded) {
            // If needed then add the {nested-content} to the evaluated params
            Expression nestedContent = ((TemplateImpl) template.get()).findExpression(this::isNestedContent);
            if (nestedContent != null) {
                // Execute the nested content first and make it accessible via the "nested-content" key
                evaluatedParams.put(NESTED_CONTENT,
                        context.execute(
                                context.resolutionContext().createChild(Mapper.wrap(evaluatedParams), null)));
            }
        }
    }

    private boolean isNestedContent(Expression expr) {
        return expr.getParts().size() == 1 && expr.getParts().get(0).getName().equals(NESTED_CONTENT);
    }

    public static class Factory extends IncludeSectionHelper.AbstractIncludeFactory<UserTagSectionHelper> {

        private static final String IT = "it";

        private final String name;
        private final String templateId;

        /**
         *
         * @param name Identifies the tag
         * @param templateId Used to locate the template
         */
        public Factory(String name, String templateId) {
            this.name = name;
            this.templateId = templateId;
        }

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(name);
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder()
                    .addParameter(Parameter.builder(IT).defaultValue(IT))
                    .addParameter(Parameter.builder(IncludeSectionHelper.Factory.ISOLATED)
                            .defaultValue(IncludeSectionHelper.Factory.ISOLATED_DEFAULT_VALUE).optional()
                            .valuePredicate(v -> IncludeSectionHelper.Factory.ISOLATED.equals(v)))
                    .build();
        }

        @Override
        protected boolean ignoreParameterInit(String key, String value) {
            // {#myTag _isolated=true /}
            return key.equals(IncludeSectionHelper.Factory.ISOLATED)
                    // {#myTag _isolated /}
                    || value.equals(IncludeSectionHelper.Factory.ISOLATED)
                    // IT with default value, e.g. {#myTag foo=bar /}
                    || (key.equals(IT) && value.equals(IT));
        }

        @Override
        protected String getTemplateId(SectionInitContext context) {
            return templateId;
        }

        @Override
        protected UserTagSectionHelper newHelper(Supplier<Template> template, Map<String, Expression> params,
                Map<String, SectionBlock> extendingBlocks, boolean isolated, SectionInitContext context) {
            boolean isNestedContentNeeded = !context.getBlock(SectionHelperFactory.MAIN_BLOCK_NAME).isEmpty();
            return new UserTagSectionHelper(template, extendingBlocks, params, isolated, isNestedContentNeeded);
        }

        @Override
        protected void handleParamInit(String key, String value, SectionInitContext context, Map<String, Expression> params) {
            if (key.equals(IT)) {
                if (value.equals(IT)) {
                    return;
                } else if (isSinglePart(value)) {
                    // Also register the param with a defaulted key
                    params.put(value, context.getExpression(key));
                }
            }
            super.handleParamInit(key, value, context, params);
        }

    }

}
