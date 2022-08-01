package io.quarkus.qute.generator;

import io.quarkus.qute.TemplateData;

@TemplateData(properties = true)
public class MyItem {

    public String id = "foo";

    public String getBar(int limit) {
        return "bar";
    }

}
