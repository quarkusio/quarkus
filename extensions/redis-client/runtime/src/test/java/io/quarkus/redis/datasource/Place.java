package io.quarkus.redis.datasource;

import java.util.Objects;

public class Place {

    public static final Place crussol = new Place("chateau de Crussol", 4);
    public static final Place grignan = new Place("chateau de Grignan", 3);
    public static final Place suze = new Place("chateau de Suze La Rousse", 2);
    public static final Place adhemar = new Place("chateau des Adhemar", 1);

    public String name;
    public int rating;

    public Place(String name, int rating) {
        this.name = name;
        this.rating = rating;
    }

    public Place() {
        // Used by the mapper.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Place place = (Place) o;
        return rating == place.rating && Objects.equals(name, place.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, rating);
    }

    @Override
    public String toString() {
        return "Place{" + "name='" + name + '\'' + ", rating=" + rating + '}';
    }
}
