package io.quarkus.it.hibernate.processor.data.puother;

import jakarta.data.repository.Repository;

@Repository(dataStore = "other")
public interface ParentMethodSecurityMyOtherRepository extends ParentMethodSecurityParentMyOtherRepository {

}
