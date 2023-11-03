package io.quarkus.qute;

public enum ParserError implements ErrorCode {

    GENERAL_ERROR,

    /**
     * <code>{fo\to}</code>
     */
    INVALID_IDENTIFIER,

    /**
     * <code>{data: }</code>
     */
    EMPTY_EXPRESSION,

    /**
     * <code>{#include /}</code>
     */
    MANDATORY_SECTION_PARAMS_MISSING,

    /**
     * <code>{#if 'foo is null}{/}</code>
     */
    UNTERMINATED_STRING_LITERAL,

    /**
     * <code>{# foo=1 /}</code>
     */
    NO_SECTION_NAME,

    /**
     * <code>{#foo test /}</code> and no helper registered for {@code foo}
     */
    NO_SECTION_HELPER_FOUND,

    /**
     * <code>{#if test}Hello {name}!{/for}</code>
     */
    SECTION_END_DOES_NOT_MATCH_START,

    /**
     * <code>{#if test}Hello{#else}Hi{/elsa}{/if}</code>
     */
    SECTION_BLOCK_END_DOES_NOT_MATCH_START,

    /**
     * <code>{#if true}Bye...{/if} Hello {/if}</code>
     */
    SECTION_START_NOT_FOUND,

    /**
     * <code>{@com.foo.Foo }</code>
     */
    INVALID_PARAM_DECLARATION,

    /**
     * <code>{#if test}Hello {name}</code>
     */
    UNTERMINATED_SECTION,

    /**
     * <code>{name</code>
     */
    UNTERMINATED_EXPRESSION,

    /**
     * <code>{#if (foo || bar}{/}</code>
     */
    UNTERMINATED_STRING_LITERAL_OR_COMPOSITE_PARAMETER,

    /**
     * <code>{foo.baz()(}</code>
     */
    INVALID_VIRTUAL_METHOD,

    /**
     * <code>{foo.baz[}</code>
     */
    INVALID_BRACKET_EXPRESSION,

    /**
     * <code>{foo[bar]}</code>
     */
    INVALID_VALUE_BRACKET_NOTATION,

    ;

    @Override
    public String getName() {
        return "PARSER_" + name();
    }

}
