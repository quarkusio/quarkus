package io.quarkus.spring.data.deployment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, String> {

    @Modifying
    @Query("UPDATE User u SET u.loginCounter = COALESCE(u.loginCounter, 0) + 1 WHERE u.userId = ?1")
    void incrementLoginCounterPlain(String userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.loginCounter = COALESCE(u.loginCounter, 0) + 1 WHERE u.userId = ?1")
    void incrementLoginCounterAutoClear(String userId);

    @Modifying
    @Query("UPDATE LoginEvent e SET e.processed = true")
    void processLoginEventsPlain();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE LoginEvent e SET e.processed = true")
    void processLoginEventsPlainAutoClearAndFlush();
}
