package io.quarkus.jsonb.deployment;

import jakarta.json.bind.annotation.JsonbTypeAdapter;

@JsonbTypeAdapter(AlphaAdapter.class)
public class Alpha {

    private String name;

    public Alpha(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
