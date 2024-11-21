package io.quarkus.spring.data.deployment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // purposely without @Param to also test fallback to compiled parameter name
    User getUserByFullNameUsingNamedQuery(String name);

    // purposely with compiled parameter name not matching the query to also test that @Param takes precedence
    User getUserByFullNameUsingNamedQueries(@Param("name") String arg);

    // issue 34395: This method is used to test the MethodNameParser class. See MethodNameParserTest class
    long countUsersByLoginEvents_Id(long id);

    List<User> findAllByLoginEvents_Id(long id);
}
