package io.quarkus.example.test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.example.infinispancachejpa.correctness.Family;
import io.quarkus.example.infinispancachejpa.correctness.Member;
import io.quarkus.example.infinispancachejpa.correctness.readwrite.FamilyRW;
import io.quarkus.example.infinispancachejpa.correctness.readwrite.MemberRW;
import io.quarkus.test.QuarkusUnitTest;

/**
 * For logging, run with: -Dorg.jboss.logging.provider=log4j2
 */
@Disabled
public class InfinispanCacheJPANonStrictCorrectnessTest {

    @Inject
    EntityManagerFactory entityManagerFactory;

    private InfinispanCacheJPACorrectnessTestCase testCase;

    @PostConstruct
    void init() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);

        // Collections don't have integrated version, these piggyback on parent's owner version (for DB).
        // However, this version number isn't extractable and is not passed to cache methods.
        // Hence, just use the same entities as defined for read-write.
        // Entity level cache concurrency is set by configuration file.

        Function<Family, ? extends Member> memberCtor = family -> new MemberRW(Utils.randomString(), family);
        Supplier<Family> familyCtor = () -> {
            String familyName = Utils.randomString();
            FamilyRW f = new FamilyRW(familyName);
            Set<MemberRW> members = new HashSet<>();
            members.add((MemberRW) memberCtor.apply(f));
            f.setMembers(members);
            return f;
        };

        testCase = new InfinispanCacheJPACorrectnessTestCase(sessionFactory, FamilyRW.class, memberCtor, familyCtor);
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource("META-INF/nonstrict-persistence.xml", "persistence.xml")
                    .addAsManifestResource("META-INF/nonstrict-microprofile-config.properties",
                            "microprofile-config.properties"));

    @Test
    public void test() throws Exception {
        testCase.test();
    }

}
