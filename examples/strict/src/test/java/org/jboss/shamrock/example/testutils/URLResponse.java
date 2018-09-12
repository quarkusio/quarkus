package org.jboss.shamrock.example.testutils;

import java.io.InputStream;

import javax.json.JsonReader;

public interface URLResponse {

    String asString();

    InputStream asInputStream();

    JsonReader asJsonReader();
}
