package io.quarkus.it.qute;

import javax.enterprise.event.Observes;
import javax.transaction.Transactional;

import io.quarkus.runtime.StartupEvent;

public class Brewery {

    @Transactional
    void onStart(@Observes StartupEvent event) {
        Beer myBeer = new Beer();
        myBeer.name = "Pilsner";
        myBeer.completed = true;
        myBeer.done = true;
        myBeer.persist();
    }

}
