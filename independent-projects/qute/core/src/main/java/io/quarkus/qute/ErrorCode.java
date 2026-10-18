package io.quarkus.qute;

/**
 * Represents a unique error code.
 *
 * @see TemplateException
 */
public interface ErrorCode {

    /**
     * Implementations are encouraged to use a prefix for a group of related problems, i.e. the parser error codes start with
     * {@code PARSER_}.
     *
     * @return the unique name
     */
    String getName();

}
