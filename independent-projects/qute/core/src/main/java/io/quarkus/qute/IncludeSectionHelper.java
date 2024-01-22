package io.quarkus.qute;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import io.quarkus.qute.Template.Fragment;

public class IncludeSectionHelper implements SectionHelper {

    static final String DEFAULT_NAME = "$default$";
    private static final String TEMPLATE = "template";
    private static final Map<String, Object> FRAGMENT_PARAMS = Map.of(Template.Fragment.ATTRIBUTE, true);

    protected final Supplier<Template> template;
    protected final Map<String, SectionBlock> extendingBlocks;
    protected final Map<String, Expression> parameters;
    protected final boolean isIsolated;

    public IncludeSectionHelper(Supplier<Template> templateSupplier, Map<String, SectionBlock> extendingBlocks,
            Map<String, Expression> parameters, boolean isIsolated) {
        this.template = templateSupplier;
        this.extendingBlocks = extendingBlocks;
        this.parameters = parameters;
        this.isIsolated = isIsolated;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        if (parameters.isEmpty() && optimizeIfNoParams()) {
            // No params
            Template t = template.get();
            SectionNode root = ((TemplateImpl) t).root;
            ResolutionContext resolutionContext;
            if (isIsolated) {
                resolutionContext = context.newResolutionContext(null, extendingBlocks);
            } else if (extendingBlocks.isEmpty()) {
                // No params and no extending blocks: {#include foo /}
                resolutionContext = context.resolutionContext();
            } else {
                resolutionContext = context.resolutionContext().createChild(null, extendingBlocks);
            }
            return root.resolve(resolutionContext, t.isFragment() ? FRAGMENT_PARAMS : null);
        } else {
            CompletableFuture<ResultNode> result = new CompletableFuture<>();
            context.evaluate(parameters).whenComplete((evaluatedParams, t1) -> {
                if (t1 != null) {
                    result.completeExceptionally(t1);
                } else {
                    addAdditionalEvaluatedParams(context, evaluatedParams);
                    try {
                        ResolutionContext resolutionContext;
                        // Execute the template with the params as the root context object
                        Object data = Mapper.wrap(evaluatedParams);
                        if (isIsolated) {
                            resolutionContext = context.newResolutionContext(data, extendingBlocks);
                        } else {
                            resolutionContext = context.resolutionContext().createChild(data, extendingBlocks);
                        }
                        Template t = template.get();
                        SectionNode root = ((TemplateImpl) t).root;
                        // Execute the template with the params as the root context object
                        root.resolve(resolutionContext, t.isFragment() ? FRAGMENT_PARAMS : null)
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

    protected boolean optimizeIfNoParams() {
        return true;
    }

    protected void addAdditionalEvaluatedParams(SectionResolutionContext context, Map<String, Object> evaluatedParams) {
        // no-op
    }

    public static class Factory extends AbstractIncludeFactory<IncludeSectionHelper> {

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of("include");
        }

        @Override
        public ParametersInfo getParameters() {
            ParametersInfo.Builder builder = ParametersInfo.builder().addParameter(TEMPLATE);
            addDefaultParams(builder);
            return builder.build();
        }

        @Override
        public MissingEndTagStrategy missingEndTagStrategy() {
            return MissingEndTagStrategy.BIND_TO_PARENT;
        }

        @Override
        protected boolean ignoreParameterInit(Supplier<String> firstParamSupplier, String key, String value) {
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
            String templateParam = context.getParameter(TEMPLATE);
            if (LiteralSupport.isStringLiteralSeparator(templateParam.charAt(0))) {
                templateParam = templateParam.substring(1, templateParam.length() - 1);
            }
            return templateParam;
        }

        @Override
        protected IncludeSectionHelper newHelper(Supplier<Template> template, Map<String, Expression> params,
                Map<String, SectionBlock> extendingBlocks, Boolean isolatedValue, SectionInitContext context) {
            return new IncludeSectionHelper(template, extendingBlocks, params, isolatedValue != null ? isolatedValue
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
                    .addParameter(Parameter.builder(ISOLATED).defaultValue(isolatedDefaultValue()).optional()
                            .valuePredicate(ISOLATED::equals).build())
                    .addParameter(Parameter.builder(UNISOLATED).optional().valuePredicate(UNISOLATED::equals).build())
                    .addParameter(Parameter.builder(IGNORE_FRAGMENTS).defaultValue(Boolean.FALSE.toString()).optional()
                            .valuePredicate(IGNORE_FRAGMENTS::equals).build())
                    .build();
        }

        @Override
        public Scope initializeBlock(Scope outerScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                for (Entry<String, String> entry : block.getParameters().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    handleParam(key, value, () -> block.getParameter(0), (k, v) -> block.addExpression(k, v));
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
                extendingBlocks = Collections.emptyMap();
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

            Map<String, Expression> params;
            if (context.getParameters().size() == 1) {
                params = Collections.emptyMap();
            } else {
                params = new HashMap<>();
                for (Entry<String, String> entry : context.getParameters().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (value.equals(ISOLATED)) {
                        isolatedValue = true;
                        continue;
                    } else if (value.equals(UNISOLATED)) {
                        isolatedValue = false;
                        continue;
                    }
                    if (value.equals(IGNORE_FRAGMENTS)) {
                        ignoreFragments = true;
                        continue;
                    }
                    handleParam(key, value, () -> context.getParameter(0), (k, v) -> params.put(k, context.getExpression(k)));
                }
            }

            // foo - no fragment
            // foo$bar - template "foo" and fragment "bar"
            // $fragment_01 - current template and fragment "fragment_01"
            String templateId = getTemplateId(context);

            //
            if (!ignoreFragments) {
                ignoreFragments = Boolean.parseBoolean(context.getParameterOrDefault(IGNORE_FRAGMENTS, "false"));
            }
            final String fragmentId = ignoreFragments ? null : getFragmentId(templateId, context);
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
            final String finalTemplateId = templateId;

            final Engine engine = context.getEngine();
            Supplier<Template> template = new Supplier<Template>() {
                @Override
                public Template get() {
                    Template template;
                    if (currentTemplate != null) {
                        template = currentTemplate.get();
                    } else {
                        template = engine.getTemplate(finalTemplateId);
                    }
                    if (template == null) {
                        throw engine.error("included template [{templateId}] not found")
                                .code(Code.TEMPLATE_NOT_FOUND)
                                .argument("templateId", finalTemplateId)
                                .origin(context.getOrigin())
                                .build();
                    }
                    if (fragmentId != null) {
                        Fragment fragment = template.getFragment(fragmentId);
                        if (fragment == null) {
                            throw engine.error("fragment [{fragmentId}] not found in the included template [{templateId}]")
                                    .code(Code.FRAGMENT_NOT_FOUND)
                                    .argument("templateId", finalTemplateId)
                                    .argument("fragmentId", fragmentId)
                                    .origin(context.getOrigin())
                                    .build();
                        }
                        template = fragment;
                    }
                    return template;
                }
            };
            return newHelper(template, params, extendingBlocks, isolatedValue, context);
        }

        protected abstract String getTemplateId(SectionInitContext context);

        protected String getFragmentId(String templateId, SectionInitContext context) {
            int idx = templateId.lastIndexOf('$');
            if (idx != -1) {
                // the part after the last occurence of a dollar sign is the fragment identifier
                String fragmentId = templateId.substring(idx + 1, templateId.length());
                if (FragmentSectionHelper.Factory.FRAGMENT_PATTERN.matcher(fragmentId).matches()) {
                    return fragmentId;
                } else {
                    throw context.getEngine().error("invalid fragment identifier [{fragmentId}]")
                            .code(Code.INVALID_FRAGMENT_ID)
                            .argument("fragmentId", fragmentId)
                            .origin(context.getOrigin())
                            .build();
                }
            }
            return null;
        }

        protected void handleParam(String key, String value, Supplier<String> firstParamSupplier,
                BiConsumer<String, String> paramConsumer) {
            if (ignoreParameterInit(firstParamSupplier, key, value)) {
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

        protected boolean ignoreParameterInit(Supplier<String> firstParamSupplier, String key, String value) {
            return key.equals(IGNORE_FRAGMENTS);
        }

        protected abstract T newHelper(Supplier<Template> template, Map<String, Expression> params,
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

}
