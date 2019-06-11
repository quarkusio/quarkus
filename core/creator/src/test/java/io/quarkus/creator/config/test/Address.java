package io.quarkus.creator.config.test;

/**
 *
 * @author Alexey Loubyansky
 */
public class Address {

    public static class Builder {

        protected String street;
        protected String zip;
        protected String city;

        private Builder() {
        }

        public Builder setStreet(String street) {
            this.street = street;
            return this;
        }

        public Builder setZip(String zip) {
            this.zip = zip;
            return this;
        }

        public Builder setCity(String city) {
            this.city = city;
            return this;
        }

        public Address build() {
            return new Address(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    protected String street;
    protected String zip;
    protected String city;

    private Address(Builder builder) {
        this.street = builder.street;
        this.zip = builder.zip;
        this.city = builder.city;
    }

    public String getStreet() {
        return street;
    }

    public String getZip() {
        return zip;
    }

    public String getCity() {
        return city;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((city == null) ? 0 : city.hashCode());
        result = prime * result + ((street == null) ? 0 : street.hashCode());
        result = prime * result + ((zip == null) ? 0 : zip.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Address other = (Address) obj;
        if (city == null) {
            if (other.city != null)
                return false;
        } else if (!city.equals(other.city))
            return false;
        if (street == null) {
            if (other.street != null)
                return false;
        } else if (!street.equals(other.street))
            return false;
        if (zip == null) {
            if (other.zip != null)
                return false;
        } else if (!zip.equals(other.zip))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[street=" + street + ", zip=" + zip + ", city=" + city + "]";
    }
}