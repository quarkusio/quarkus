package io.quarkus.spring.data.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ModifyingQueryWithFlushAndClearTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("import_users.sql", "import.sql")
                    .addClasses(User.class, LoginEvent.class, UserRepository.class))
            .withConfigurationResource("application.properties");

    @Inject
    UserRepository repo;

    @BeforeEach
    @Transactional
    public void setUp() {
        final User user = getUser("JOHN");
        user.setLoginCounter(0);
        repo.save(user);
    }

    @Test
    @Transactional
    public void testNoAutoClear() {
        getUser("JOHN"); // read user to attach it to entity manager

        repo.incrementLoginCounterPlain("JOHN");

        final User userAfterIncrement = getUser("JOHN"); // we get the cached entity
        // the read doesn't re-read the incremented counter and is therefore equal to the old value
        assertThat(userAfterIncrement.getLoginCounter()).isEqualTo(0);
    }

    @Test
    @Transactional
    public void testAutoClear() {
        getUser("JOHN"); // read user to attach it to entity manager

        repo.incrementLoginCounterAutoClear("JOHN");

        final User userAfterIncrement = getUser("JOHN");
        assertThat(userAfterIncrement.getLoginCounter()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void testNoAutoFlush() {
        final User user = getUser("JOHN");
        createLoginEvent(user);

        repo.processLoginEventsPlain();

        final User verifyUser = getUser("JOHN");
        // processLoginEvents did not see the new login event
        final boolean allProcessed = verifyUser.getLoginEvents().stream()
                .allMatch(loginEvent -> loginEvent.isProcessed());
        assertThat(allProcessed).describedAs("all LoginEvents are marked as processed").isFalse();
    }

    @Test
    @Transactional
    public void testAutoFlush() {
        final User user = getUser("JOHN");
        createLoginEvent(user);

        repo.processLoginEventsPlainAutoClearAndFlush();

        final User verifyUser = getUser("JOHN");
        final boolean allProcessed = verifyUser.getLoginEvents().stream()
                .allMatch(loginEvent -> loginEvent.isProcessed());
        assertThat(allProcessed).describedAs("all LoginEvents are marked as processed").isTrue();
    }

    @Test
    @Transactional
    public void testNamedQueryOnEntities() {
        User user = repo.getUserByFullNameUsingNamedQuery("John Doe");
        assertThat(user).isNotNull();
    }

    @Test
    @Transactional
    public void testNamedQueriesOnEntities() {
        User user = repo.getUserByFullNameUsingNamedQueries("John Doe");
        assertThat(user).isNotNull();
    }

    private LoginEvent createLoginEvent(User user) {
        final LoginEvent loginEvent = new LoginEvent();
        loginEvent.setUser(user);
        loginEvent.setZonedDateTime(ZonedDateTime.now());
        user.addEvent(loginEvent);
        return loginEvent;
    }

    private User getUser(String userId) {
        final Optional<User> user = repo.findById(userId);
        assertThat(user).describedAs("user <%s>", userId).isPresent();
        return user.get();
    }

}
