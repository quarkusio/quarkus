package io.quarkus.creator.config.test;

/**
 *
 * @author Alexey Loubyansky
 */
class Person {

    protected String firstName;
    protected String lastName;
    protected Address home;
    protected Address work;

    public void setLastName(String name) {
        this.lastName = name;
    }

    public void setFirstName(String name) {
        this.firstName = name;
    }

    public void setHomeAddress(Address address) {
        this.home = address;
    }

    public void setWorkAddress(Address address) {
        this.work = address;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Address getHomeAddress() {
        return home;
    }

    public Address getWorkAddress() {
        return work;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
        result = prime * result + ((home == null) ? 0 : home.hashCode());
        result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
        result = prime * result + ((work == null) ? 0 : work.hashCode());
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
        Person other = (Person) obj;
        if (firstName == null) {
            if (other.firstName != null)
                return false;
        } else if (!firstName.equals(other.firstName))
            return false;
        if (home == null) {
            if (other.home != null)
                return false;
        } else if (!home.equals(other.home))
            return false;
        if (lastName == null) {
            if (other.lastName != null)
                return false;
        } else if (!lastName.equals(other.lastName))
            return false;
        if (work == null) {
            if (other.work != null)
                return false;
        } else if (!work.equals(other.work))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[firstName=" + firstName + ", lastName=" + lastName + ", home=" + home + ", work=" + work + "]";
    }
}