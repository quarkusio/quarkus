package io.quarkus.qute;

public interface ParserHook {

    /**
     * 
     * @param parserHelper
     */
    void beforeParsing(ParserHelper parserHelper);

}
