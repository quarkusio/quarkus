package io.quarkus.spring.data.deployment;

import java.time.MonthDay;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForecastRepository extends CrudRepository<Forecast, Integer> {

    /**
     * Mirrors the query from #51750: a projection whose element type is a JDK class that
     * Hibernate ORM does not provide natively and that is not in the Jandex index. This
     * used to fail the build with IllegalStateException.
     */
    @Query("SELECT DISTINCT f.renewal FROM Forecast f WHERE f.amount <> 0 ORDER BY f.renewal")
    List<MonthDay> findDistinctRenewalsWithNonZeroForecasts();
}
