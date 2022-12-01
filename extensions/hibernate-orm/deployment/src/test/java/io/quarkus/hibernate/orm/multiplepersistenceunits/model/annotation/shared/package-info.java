@PersistenceUnit(PersistenceUnit.DEFAULT)
@PersistenceUnit("users")
@PersistenceUnit("inventory")
package io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.shared;

import io.quarkus.hibernate.orm.PersistenceUnit;