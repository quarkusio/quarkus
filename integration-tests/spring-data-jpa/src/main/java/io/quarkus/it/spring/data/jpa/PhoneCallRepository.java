package io.quarkus.it.spring.data.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneCallRepository extends JpaRepository<PhoneCall, PhoneNumberId> {

    PhoneCall findByIdAreaCode(String areaCode);
}
