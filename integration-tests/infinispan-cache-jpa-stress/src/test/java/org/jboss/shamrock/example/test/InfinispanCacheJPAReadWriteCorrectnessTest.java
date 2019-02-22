package org.jboss.shamrock.example.test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.jboss.shamrock.example.infinispancachejpa.correctness.Family;
import org.jboss.shamrock.example.infinispancachejpa.correctness.Member;
import org.jboss.shamrock.example.infinispancachejpa.correctness.readwrite.FamilyRW;
import org.jboss.shamrock.example.infinispancachejpa.correctness.readwrite.MemberRW;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * For logging, run with: -Dorg.jboss.logging.provider=log4j2
 */
@Disabled
public class InfinispanCacheJPAReadWriteCorrectnessTest {

    @Inject
    EntityManagerFactory entityManagerFactory;

    private InfinispanCacheJPACorrectnessTestCase testCase;

    @PostConstruct
    void init() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);

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
    static ShamrockUnitTest runner = new ShamrockUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource("META-INF/readwrite-persistence.xml", "persistence.xml")
                    .addAsManifestResource("META-INF/readwrite-microprofile-config.properties",
                            "microprofile-config.properties"));

    @Test
    public void test() throws Exception {
        testCase.test();
    }

}
