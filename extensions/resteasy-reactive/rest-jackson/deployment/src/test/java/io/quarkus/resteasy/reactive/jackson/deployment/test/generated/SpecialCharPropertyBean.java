package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpecialCharPropertyBean {

    @JsonProperty("ROUND-0.2")
    private int roundValue;

    @JsonProperty("normal_name")
    private String normalName;

    public int getRoundValue() {
        return roundValue;
    }

    public void setRoundValue(int roundValue) {
        this.roundValue = roundValue;
    }

    public String getNormalName() {
        return normalName;
    }

    public void setNormalName(String normalName) {
        this.normalName = normalName;
    }
}
