package io.quarkus.resteasy.reactive.jackson.deployment.test;

import io.quarkus.resteasy.reactive.jackson.SecureField;

public class FrogBodyParts {

    public FrogBodyParts() {
    }

    public FrogBodyParts(String bodyPartName) {
        this.parts = new BodyPart[] { new BodyPart(bodyPartName) };
    }

    private BodyPart[] parts;

    public BodyPart[] getParts() {
        return parts;
    }

    public void setParts(BodyPart[] parts) {
        this.parts = parts;
    }

    public static class BodyPart {

        public BodyPart(String name) {
            this.name = name;
        }

        public BodyPart() {
        }

        @SecureField(rolesAllowed = "admin")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
