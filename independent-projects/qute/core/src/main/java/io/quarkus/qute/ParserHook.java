package io.quarkus.qute;

public interface ParserHook {

    void beforeParsing(ParserHelper parser, String id);

}
