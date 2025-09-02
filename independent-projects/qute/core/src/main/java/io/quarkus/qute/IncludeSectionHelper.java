package io.quarkus.qute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import io.quarkus.qute.SectionHelperFactory.SectionInitContext;
import io.quarkus.qute.Template.Fragment;
import io.quarkus.qute.TemplateNode.Origin;

public class IncludeSectionHelper implements SectionHelper {

    static final String DEFAULT_NAME = "$default$";
    private static final String TEMPLATE = "template";
    private static final Map<String, Object> FRAGMENT_PARAMS = Map.of(Template.Fragment.ATTRIBUTE, true);

    protected final TemplateSupplier templateSupplier;
    protected final Map<String, SectionBlock> extendingBlocks;
    protected final Map<String, Expression> parameters;
    protected final boolean isIsolated;

    IncludeSectionHelper(TemplateSupplier templateSupplier, Map<String, SectionBlock> extendingBlocks,
            Map<String, Expression> parameters, boolean isIsolated) {
        this.templateSupplier = templateSupplier;
        this.extendingBlocks = extendingBlocks;
        this.parameters = parameters;
        this.isIsolated = isIsolated;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        if (parameters.isEmpty() && optimizeIfNoParams()) {
            // No params
            Template template = templateSupplier.get(Map.of());
            SectionNode root = template.getRootNode();
            ResolutionContext resolutionContext;
            if (isIsolated) {
                resolutionContext = context.newResolutionContext(null, extendingBlocks);
            } else if (extendingBlocks.isEmpty()) {
                // No params and no extending blocks: {#include foo /}
                resolutionContext = context.resolutionContext();
            } else {
                resolutionContext = context.resolutionContext().createChild(null, extendingBlocks);
            }
            return root.resolve(resolutionContext, template.isFragment() ? FRAGMENT_PARAMS : null);
        } else {
            CompletableFuture<ResultNode> result = new CompletableFuture<>();
            context.evaluate(parameters).whenComplete((evaluatedParams, t1) -> {
                if (t1 != null) {
                    result.completeExceptionally(t1);
                } else {
                    addAdditionalEvaluatedParams(context, evaluatedParams);
                    try {
                        ResolutionContext resolutionContext;
                        Object data = Mapper.wrap(evaluatedParams);
                        if (isIsolated) {
                            resolutionContext = context.newResolutionContext(data, extendingBlocks);
                        } else {
                            resolutionContext = context.resolutionContext().createChild(data, extendingBlocks);
                        }

                        Template template = templateSupplier.get(evaluatedParams);
                        SectionNode root = template.getRootNode();

                        // Execute the template with the params as the root context object
                        root.resolve(resolutionContext, template.isFragment() ? FRAGMENT_PARAMS : null)
                                .whenComplete((resultNode, t2) -> {
                                    if (t2 != null) {
                                        result.completeExceptionally(t2);
                                    } else {
                                        result.complete(resultNode);
                                    }
                                });
                    } catch (Throwable e) {
                        result.completeExceptionally(e);
                    }
                }
            });
            return result;
        }
    }

    public Map<String, Expression> getParameters() {
        return parameters;
    }

    public boolean isIsolated() {
        return isIsolated;
    }

    protected boolean optimizeIfNoParams() {
        return true;
    }

    protected void addAdditionalEvaluatedParams(SectionResolutionContext context, Map<String, Object> evaluatedParams) {
        // no-op
    }

    public static class Factory extends AbstractIncludeFactory<IncludeSectionHelper> {

        static final String DYNAMIC_ID = "_id";

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of("include");
        }

        @Override
        public ParametersInfo getParameters() {
            ParametersInfo.Builder builder = ParametersInfo.builder()
                    .addParameter(Parameter.builder(TEMPLATE)
                            .optional()
                            .valuePredicate(this::skipBuiltInParam)
                            .build())
                    .addParameter(Parameter.builder(DYNAMIC_ID)
                            .optional()
                            .valuePredicate(this::skipBuiltInParam)
                            .build());
            addDefaultParams(builder);
            return builder.build();
        }

        @Override
        public MissingEndTagStrategy missingEndTagStrategy() {
            return MissingEndTagStrategy.BIND_TO_PARENT;
        }

        @Override
        protected boolean ignoreParameterInit(Map<String, String> params, Supplier<String> firstParamValue, String key,
                String value) {
            return key.equals(TEMPLATE)
                    // {#include foo _isolated=true /}
                    || key.equals(ISOLATED)
                    // {#include foo _isolated /}
                    || value.equals(ISOLATED)
                    // {#include foo _unisolated /}
                    || value.equals(UNISOLATED)
                    // {#include foo _ignoreFragments=true /}
                    || key.equals(IGNORE_FRAGMENTS)
                    // {#include foo _ignoreFragments /}
                    || value.equals(IGNORE_FRAGMENTS);
        }

        @Override
        protected String getTemplateId(SectionInitContext context) {
            if (context.getParameters().containsKey(DYNAMIC_ID)) {
                return null;
            }
            String templateParam = context.getParameter(TEMPLATE);
            if (templateParam == null) {
                throw context.error("Neither the template id nor the template name parameter was specified")
                        .build();
            }
            if (LiteralSupport.isStringLiteralSeparator(templateParam.charAt(0))) {
                templateParam = templateParam.substring(1, templateParam.length() - 1);
            }
            return templateParam;
        }

        @Override
        protected IncludeSectionHelper newHelper(TemplateSupplier templateSupplier, Map<String, Expression> params,
                Map<String, SectionBlock> extendingBlocks, Boolean isolatedValue, SectionInitContext context) {
            return new IncludeSectionHelper(templateSupplier, extendingBlocks, params, isolatedValue != null ? isolatedValue
                    : Boolean.parseBoolean(context.getParameterOrDefault(ISOLATED, Boolean.FALSE.toString())));
        }

    }

    static abstract class AbstractIncludeFactory<T extends SectionHelper> implements SectionHelperFactory<T> {

        static final String ISOLATED = "_isolated";
        static final String UNISOLATED = "_unisolated";
        static final String IGNORE_FRAGMENTS = "_ignoreFragments";
        static final Pattern WHITESPACE = Pattern.compile("\\s");

        @Override
        public boolean treatUnknownSectionsAsBlocks() {
            return true;
        }

        String isolatedDefaultValue() {
            return Boolean.FALSE.toString();
        }

        void addDefaultParams(ParametersInfo.Builder builder) {
            builder
                    .addParameter(Parameter.builder(ISOLATED)
                            .defaultValue(isolatedDefaultValue())
                            .optional()
                            .valuePredicate(ISOLATED::equals)
                            .build())
                    .addParameter(Parameter.builder(UNISOLATED)
                            .optional()
                            .valuePredicate(UNISOLATED::equals)
                            .build())
                    .addParameter(Parameter.builder(IGNORE_FRAGMENTS)
                            .defaultValue(Boolean.FALSE.toString())
                            .optional()
                            .valuePredicate(IGNORE_FRAGMENTS::equals)
                            .build())
                    .build();
        }

        protected boolean skipBuiltInParam(String value) {
            return value != null
                    && !ISOLATED.equals(value)
                    && !UNISOLATED.equals(value)
                    && !IGNORE_FRAGMENTS.equals(value);
        }

        @Override
        public Scope initializeBlock(Scope outerScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                Map<String, String> params = block.getParameters();
                for (Entry<String, String> entry : params.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    processParam(params, key, value, () -> block.getParameter(0), (k, v) -> block.addExpression(k, v));
                }
                return outerScope;
            } else {
                return outerScope;
            }
        }

        @Override
        public T initialize(SectionInitContext context) {
            boolean isEmpty = context.getBlocks().size() == 1 && context.getBlocks().get(0).isEmpty();
            Boolean isolatedValue = null;
            boolean ignoreFragments = false;
            Map<String, SectionBlock> extendingBlocks;

            if (isEmpty) {
                extendingBlocks = Map.of();
            } else {
                extendingBlocks = new HashMap<>();
                for (SectionBlock block : context.getBlocks()) {
                    String name = block.id.equals(MAIN_BLOCK_NAME) ? DEFAULT_NAME : block.label;
                    if (extendingBlocks.put(name, block) != null) {
                        throw block.error("multiple blocks define the content for the \\{#insert\\} section of name [{name}]")
                                .code(Code.MULTIPLE_INSERTS_OF_NAME)
                                .origin(context.getOrigin())
                                .argument("name", name)
                                .build();
                    }
                }
            }

            Map<String, Expression> params = new HashMap<>();
            Map<String, String> contextParams = context.getParameters();
            for (Entry<String, String> entry : contextParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value.equals(ISOLATED)) {
                    // {#include foo _isolated /}
                    isolatedValue = true;
                    continue;
                } else if (value.equals(UNISOLATED)) {
                    // {#include foo _unisolated /}
                    isolatedValue = false;
                    continue;
                }
                if (value.equals(IGNORE_FRAGMENTS)) {
                    // {#include foo _ignoreFragments /}
                    ignoreFragments = true;
                    continue;
                }
                processParam(contextParams, key, value, () -> context.getParameter(0),
                        (k, v) -> params.put(k, context.getExpression(k)));
            }

            // null - dynamic lookup is needed
            // "foo" - no fragment
            // "foo$bar" - template "foo" and fragment "bar"
            // "$fragment_01" - current template and fragment "fragment_01"
            String templateId = getTemplateId(context);
            TemplateSupplier templateSupplier = templateId != null
                    ? new FixedTemplateSupplier(context, ignoreFragments, templateId)
                    : new DynamicTemplateSupplier(context, ignoreFragments);
            return newHelper(templateSupplier, params, extendingBlocks, isolatedValue,
                    context);
        }

        /**
         *
         * @param context
         * @return {@code null} if dynamic lookup is needed
         */
        protected abstract String getTemplateId(SectionInitContext context);

        protected void processParam(Map<String, String> params, String key, String value, Supplier<String> firstParamValue,
                BiConsumer<String, String> paramConsumer) {
            if (ignoreParameterInit(params, firstParamValue, key, value)) {
                return;
            } else if (useDefaultedKey(key, value)) {
                if (LiteralSupport.isStringLiteral(value)) {
                    // {#include "foo" /} => {#include foo="foo" /}
                    key = value.substring(1, value.length() - 1);
                } else {
                    key = value;
                }
            }
            paramConsumer.accept(key, value);
        }

        protected boolean useDefaultedKey(String key, String value) {
            // Use the defaulted key if:
            // 1. The key is a generated id
            // 2. The value is a single identifier
            // {#myTag user} should be equivalent to {#myTag user=user}
            return LiteralSupport.INTEGER_LITERAL_PATTERN.matcher(key).matches()
                    && isSinglePart(value);
        }

        protected boolean isSinglePart(String value) {
            return Expressions.splitParts(value).size() == 1 && !WHITESPACE.matcher(value).find();
        }

        /**
         *
         * @param params
         * @param firstParamSupplier
         * @param key
         * @param value
         * @return {@code true} if the parameter should not be processed, i.e. registered as an expression
         */
        protected boolean ignoreParameterInit(Map<String, String> params, Supplier<String> firstParamSupplier, String key,
                String value) {
            return key.equals(IGNORE_FRAGMENTS);
        }

        protected abstract T newHelper(TemplateSupplier templateSupplier, Map<String, Expression> params,
                Map<String, SectionBlock> extendingBlocks, Boolean isolatedValue, SectionInitContext context);
    }

    enum Code implements ErrorCode {

        MULTIPLE_INSERTS_OF_NAME,

        TEMPLATE_NOT_FOUND,

        FRAGMENT_NOT_FOUND,

        INVALID_FRAGMENT_ID

        ;

        @Override
        public String getName() {
            return "INCLUDE_" + name();
        }

    }

    static class FixedTemplateSupplier extends TemplateSupplier {

        private final String templateId;
        private final String fragmentId;
        private final Supplier<Template> currentTemplate;

        FixedTemplateSupplier(SectionInitContext context, boolean ignoreFragments, String templateId) {
            super(context, ignoreFragments);
            final String fragmentId = getFragmentId(templateId);
            Supplier<Template> currentTemplate;
            if (fragmentId != null) {
                // remove the trailing fragment part
                templateId = templateId.substring(0, templateId.lastIndexOf('$'));
                if (templateId.isEmpty()) {
                    // use the current template
                    currentTemplate = context.getCurrentTemplate();
                } else {
                    currentTemplate = null;
                }
            } else {
                currentTemplate = null;
            }
            this.templateId = templateId;
            this.fragmentId = fragmentId;
            this.currentTemplate = currentTemplate;
        }

        @Override
        Template get(Map<String, Object> params) {
            return doGet(templateId, fragmentId, currentTemplate);
        }

    }

    static class DynamicTemplateSupplier extends TemplateSupplier {

        private final Supplier<Template> currentTemplate;

        DynamicTemplateSupplier(SectionInitContext context, boolean ignoreFragments) {
            super(context, ignoreFragments);
            this.currentTemplate = context.getCurrentTemplate();
        }

        @Override
        Template get(Map<String, Object> params) {
            Object templateIdVal = params.get(Factory.DYNAMIC_ID);
            if (templateIdVal == null) {
                throw engine.error("dynamically included template not found")
                        .code(Code.TEMPLATE_NOT_FOUND)
                        .origin(origin)
                        .build();
            }
            String templateId = templateIdVal.toString();
            final String fragmentId = getFragmentId(templateId);
            Supplier<Template> currentTemplate;
            if (fragmentId != null) {
                // remove the trailing fragment part
                templateId = templateId.substring(0, templateId.lastIndexOf('$'));
                if (templateId.isEmpty()) {
                    // use the current template
                    currentTemplate = this.currentTemplate;
                } else {
                    currentTemplate = null;
                }
            } else {
                currentTemplate = null;
            }
            return doGet(templateId, fragmentId, currentTemplate);
        }

    }

    static abstract class TemplateSupplier {

        protected final Engine engine;
        protected final Origin origin;
        protected final boolean ignoreFragments;

        TemplateSupplier(SectionInitContext context, boolean ignoreFragments) {
            this.engine = context.getEngine();
            this.origin = context.getOrigin();
            this.ignoreFragments = ignoreFragments ? true
                    : Boolean.parseBoolean(context.getParameterOrDefault(AbstractIncludeFactory.IGNORE_FRAGMENTS, "false"));
        }

        abstract Template get(Map<String, Object> params);

        protected Template doGet(String templateId, String fragmentId, Supplier<Template> currentTemplate) {
            Template ret;
            if (currentTemplate != null) {
                ret = currentTemplate.get();
            } else {
                ret = engine.getTemplate(templateId);
            }
            if (ret == null) {
                throw engine.error("included template [{templateId}] not found")
                        .code(Code.TEMPLATE_NOT_FOUND)
                        .argument("templateId", templateId)
                        .origin(origin)
                        .build();
            }
            if (fragmentId != null) {
                Fragment fragment = ret.getFragment(fragmentId);
                if (fragment == null) {
                    throw engine.error("fragment [{fragmentId}] not found in the included template [{templateId}]")
                            .code(Code.FRAGMENT_NOT_FOUND)
                            .argument("templateId", templateId)
                            .argument("fragmentId", fragmentId)
                            .origin(origin)
                            .build();
                }
                ret = fragment;
            }
            return ret;
        }

        protected String getFragmentId(String templateId) {
            if (ignoreFragments) {
                return null;
            }
            int idx = templateId.lastIndexOf('$');
            if (idx != -1) {
                // the part after the last occurence of a dollar sign is the fragment identifier
                String fragmentId = templateId.substring(idx + 1, templateId.length());
                if (FragmentSectionHelper.Factory.FRAGMENT_PATTERN.matcher(fragmentId).matches()) {
                    return fragmentId;
                } else {
                    throw engine.error("invalid fragment identifier [{fragmentId}]")
                            .code(Code.INVALID_FRAGMENT_ID)
                            .argument("fragmentId", fragmentId)
                            .origin(origin)
                            .build();
                }
            }
            return null;
        }

    }

}
