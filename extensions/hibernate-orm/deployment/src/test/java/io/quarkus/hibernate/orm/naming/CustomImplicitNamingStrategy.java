package io.quarkus.hibernate.orm.naming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

public class CustomImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {
    @Override
    public Identifier determinePrimaryTableName(ImplicitEntityNameSource source) {
        return toIdentifier("TBL_" + source.getEntityNaming().getEntityName().replace('.', '_').toUpperCase(),
                source.getBuildingContext());
    }
}
