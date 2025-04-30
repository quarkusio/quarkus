package io.quarkus.it.kafka.jsonschema;

import io.confluent.kafka.schemaregistry.annotations.Schema;

//This class is used by both serializers, but for it to be usable by the Confluent serializer the schema must be attached here in the annotation
@Schema(value = """
        {
          "$id": "https://example.com/person.schema.json",
          "$schema": "http://json-schema.org/draft-07/schema#",
          "title": "Pet",
          "type": "object",
          "properties": {
            "name": {
              "type": "string",
              "description": "The pet's name."
            },
            "color": {
              "type": "string",
              "description": "The pet's color."
            }
          }
        }""", refs = {})
public class Pet {

    private String name;
    private String color;

    public Pet() {
    }

    public Pet(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
