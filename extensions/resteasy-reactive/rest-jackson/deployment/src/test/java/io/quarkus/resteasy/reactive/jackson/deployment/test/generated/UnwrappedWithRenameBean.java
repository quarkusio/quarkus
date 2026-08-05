package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class UnwrappedWithRenameBean {

    @JsonProperty("label")
    private String name;

    @JsonUnwrapped
    private InnerAddress address;

    @JsonIgnore
    private String hidden;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InnerAddress getAddress() {
        return address;
    }

    public void setAddress(InnerAddress address) {
        this.address = address;
    }

    public String getHidden() {
        return hidden;
    }

    public void setHidden(String hidden) {
        this.hidden = hidden;
    }

    public static class InnerAddress {

        private String city;

        @JsonProperty("zip_code")
        private String zipCode;

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }
    }
}
