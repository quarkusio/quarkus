package io.quarkus.security.jpa;

import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.wildfly.security.password.Password;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.jpa.runtime.JpaIdentityProvider;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

@Singleton
public class UserEntityIdentityProvider extends JpaIdentityProvider {

    @Override
    public SecurityIdentity authenticate(EntityManager em,
            UsernamePasswordAuthenticationRequest request) {

        Session session = em.unwrap(Session.class);
        SimpleNaturalIdLoadAccess<PlainUserEntity> naturalIdLoadAccess = session.bySimpleNaturalId(PlainUserEntity.class);
        PlainUserEntity user = naturalIdLoadAccess.load(request.getUsername());
        //        Query query = em.createQuery("FROM PlainUserEntity WHERE name = :name");
        //        query.setParameter("name", request.getUsername());
        //        PlainUserEntity user = getSingleUser(query);
        if (user == null)
            return null;

        // for MCF:
        //               Password storedPassword = getMcfPasword(user.pass);
        // for clear:
        Password storedPassword = getClearPassword(user.pass);

        QuarkusSecurityIdentity.Builder builder = checkPassword(storedPassword, request);

        addRoles(builder, user.role);
        return builder.build();
    }
}
