package io.quarkus.hibernate.validator.runtime;

import java.util.Optional;

import org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.hibernate-validator")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface HibernateValidatorBuildTimeConfig {

    /**
     * Enable the fail fast mode. When fail fast is enabled the validation will stop on the first constraint violation
     * detected.
     */
    @WithDefault("false")
    boolean failFast();

    /**
     * Method validation.
     */
    @ConfigDocSection
    HibernateValidatorMethodBuildTimeConfig methodValidation();

    /**
     * Expression Language.
     */
    @ConfigDocSection
    HibernateValidatorExpressionLanguageBuildTimeConfig expressionLanguage();

    @ConfigGroup
    public interface HibernateValidatorMethodBuildTimeConfig {

        /**
         * Define whether overriding methods that override constraints should throw a
         * {@code ConstraintDefinitionException}. The default value is {@code false}, i.e. do not allow.
         * <p>
         * See Section 4.5.5 of the JSR 380 specification, specifically <blockquote> In sub types (be it sub
         * classes/interfaces or interface implementations), no parameter constraints may be declared on overridden or
         * implemented methods, nor may parameters be marked for cascaded validation. This would pose a strengthening of
         * preconditions to be fulfilled by the caller. </blockquote>
         */
        @WithDefault("false")
        boolean allowOverridingParameterConstraints();

        /**
         * Define whether parallel methods that define constraints should throw a {@code ConstraintDefinitionException}.
         * The default value is {@code false}, i.e. do not allow.
         * <p>
         * See Section 4.5.5 of the JSR 380 specification, specifically <blockquote> If a sub type overrides/implements
         * a method originally defined in several parallel types of the hierarchy (e.g. two interfaces not extending
         * each other, or a class and an interface not implemented by said class), no parameter constraints may be
         * declared for that method at all nor parameters be marked for cascaded validation. This again is to avoid an
         * unexpected strengthening of preconditions to be fulfilled by the caller. </blockquote>
         */
        @WithDefault("false")
        boolean allowParameterConstraintsOnParallelMethods();

        /**
         * Define whether more than one constraint on a return value may be marked for cascading validation are allowed.
         * The default value is {@code false}, i.e. do not allow.
         * <p>
         * See Section 4.5.5 of the JSR 380 specification, specifically <blockquote> One must not mark a method return
         * value for cascaded validation more than once in a line of a class hierarchy. In other words, overriding
         * methods on sub types (be it sub classes/interfaces or interface implementations) cannot mark the return value
         * for cascaded validation if the return value has already been marked on the overridden method of the super
         * type or interface. </blockquote>
         */
        @WithDefault("false")
        boolean allowMultipleCascadedValidationOnReturnValues();
    }

    @ConfigGroup
    public interface HibernateValidatorExpressionLanguageBuildTimeConfig {

        /**
         * Configure the Expression Language feature level for constraints, allowing the selection of Expression
         * Language features available for message interpolation.
         * <p>
         * This property only affects the EL feature level of "static" constraint violation messages set through the
         * <code>message</code> attribute of constraint annotations.
         * <p>
         * In particular, it doesn't affect the default EL feature level for custom violations created programmatically
         * in validator implementations. The feature level for those can only be configured directly in the validator
         * implementation.
         */
        @ConfigDocDefault("bean-properties")
        Optional<ExpressionLanguageFeatureLevel> constraintExpressionFeatureLevel();
    }
}
