package io.quarkus.it.spring.data.jpa;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PhoneCallRepository extends JpaRepository<PhoneCall, PhoneNumberId> {

    PhoneCall findByIdAreaCode(String areaCode);

    @Query("select p.id from PhoneCall p")
    Set<PhoneNumberId> findAllIds();
}
