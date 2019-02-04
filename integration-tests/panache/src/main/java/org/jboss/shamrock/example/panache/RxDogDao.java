package org.jboss.shamrock.example.panache;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.panache.rx.RxDaoBase;

@ApplicationScoped
public class RxDogDao implements RxDaoBase<RxDog>{
    public final static RxDogDao INSTANCE = new RxDogDao();
}
