package io.quarkus.qute.generator;

import io.quarkus.qute.TemplateData;

@TemplateData
public class SomeBean implements SomeInterface {

    private final String image;

    SomeBean(String image) {
        this.image = image;
    }

    public String image() {
        return image;
    }

    public boolean hasImage() {
        return image != null;
    }

    public boolean hasImage(String val) {
        return image.equals(val);
    }

}
