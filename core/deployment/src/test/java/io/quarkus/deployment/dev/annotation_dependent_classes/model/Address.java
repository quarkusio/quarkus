package io.quarkus.deployment.dev.annotation_dependent_classes.model;

import java.util.List;

public class Address extends ModelBase {
    private String city;

    private String streetName;

    public LocalizationInfo localizationInfo;

    private List<Contact> contacts;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public static class LocalizationInfo {

        public static class LocalizationInfo2 {
            public static class LocalizationInfo3 {

            }
        }
    }
}
