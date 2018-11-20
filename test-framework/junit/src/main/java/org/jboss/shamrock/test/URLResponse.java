package org.jboss.shamrock.test;

import java.io.IOException;
import java.io.InputStream;

import javax.json.JsonReader;

public interface URLResponse {

    int statusCode();

    IOException exception();

    String asString();

    InputStream asInputStream();

    JsonReader asJsonReader();
}
