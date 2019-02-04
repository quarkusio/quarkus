package org.jboss.shamrock.example.panache;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.jboss.panache.rx.NotReallyJpa;
import org.jboss.panache.rx.RxModel;

import io.reactivex.Single;

@NotReallyJpa
@Entity
public class RxDog extends RxModel<RxDog> {

    public String name;
    
    public String race;
    
    @ManyToOne
    public Single<RxPerson> owner;

    public RxDog(String name, String race) {
        this.name = name;
        this.race = race;
    }
}
