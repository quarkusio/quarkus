package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "zebra", "alpha", "middle" })
public class PropertyOrderBean {

    private String alpha;
    private String middle;
    private String zebra;

    public String getAlpha() {
        return alpha;
    }

    public void setAlpha(String alpha) {
        this.alpha = alpha;
    }

    public String getMiddle() {
        return middle;
    }

    public void setMiddle(String middle) {
        this.middle = middle;
    }

    public String getZebra() {
        return zebra;
    }

    public void setZebra(String zebra) {
        this.zebra = zebra;
    }
}
