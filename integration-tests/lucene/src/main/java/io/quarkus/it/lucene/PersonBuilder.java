package io.quarkus.it.lucene;

public class PersonBuilder {
    Person p = new Person();

    public PersonBuilder withName(String name) {
        p.name = name;
        return this;
    }

    public PersonBuilder withEmail(String email) {
        p.email = email;
        return this;
    }

    public PersonBuilder withEyeDistance(Double eyeDistance) {
        p.eyeDistance = eyeDistance;
        return this;
    }

    public PersonBuilder withMetadata(String metadata) {
        p.metadata = metadata;
        return this;
    }

    public PersonBuilder withLatitude(Double latitude) {
        p.latitude = latitude;
        return this;
    }

    public PersonBuilder withLongitude(Double longitude) {
        p.longitude = longitude;
        return this;
    }

    public PersonBuilder withAge(int age) {
        p.age = age;
        return this;
    }

    public PersonBuilder withHeight(float height) {
        p.height = height;
        return this;
    }

    public PersonBuilder setCompany(String company) {
        p.company = company;
        return this;
    }

    public Person getPerson() {
        return p;
    }
}