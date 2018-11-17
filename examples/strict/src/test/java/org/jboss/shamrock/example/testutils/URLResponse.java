package org.jboss.shamrock.example.testutils;

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
