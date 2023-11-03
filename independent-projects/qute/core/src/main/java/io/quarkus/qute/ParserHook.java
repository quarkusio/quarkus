package io.quarkus.qute;

/**
 * This component can be used to hook into the parser logic.
 */
public interface ParserHook {

    /**
     * This method is invoked before a template contents is parsed
     *
     * @param parserHelper
     */
    void beforeParsing(ParserHelper parserHelper);

}
