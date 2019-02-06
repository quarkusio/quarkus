package org.jboss.shamrock.example.panache;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.jboss.panache.jpa.Model;

@Entity
public class Dog extends Model {

    public String name;
    
    public String race;
    
    @ManyToOne
    public Person owner;

    public Dog(String name, String race) {
        this.name = name;
        this.race = race;
    }
}
