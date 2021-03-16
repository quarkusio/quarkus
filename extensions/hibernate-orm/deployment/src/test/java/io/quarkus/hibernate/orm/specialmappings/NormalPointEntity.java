package io.quarkus.hibernate.orm.specialmappings;

import javax.persistence.Entity;

@Entity
public class NormalPointEntity extends PointEntity {

    private String place;

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }
}
