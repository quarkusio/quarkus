package io.quarkus.funqy.test.util;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

public class EventDataProvider {

    public static String getData(String path) {
        try {
            return IOUtils.toString(
                    EventDataProvider.class.getClassLoader().getResourceAsStream("events/" + path),
                    Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
