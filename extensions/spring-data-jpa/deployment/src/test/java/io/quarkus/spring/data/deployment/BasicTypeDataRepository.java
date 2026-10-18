package io.quarkus.spring.data.deployment;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BasicTypeDataRepository extends JpaRepository<BasicTypeData, Integer> {

    @Query("select doubleValue from BasicTypeData where url = ?1")
    Double doubleByURL(URL url);

    @Query("select duration from BasicTypeData where uuid = ?1")
    Duration durationByUUID(UUID uuid);

    @Query("select timeZone from BasicTypeData where locale = ?1")
    Set<TimeZone> timeZonesByLocale(Locale locale);

    @Query(value = "select id, doubleValue from BasicTypeData")
    List<WithDoubleValue> findAllDoubleValuesToList();

    @Query(value = "select id, doubleValue from BasicTypeData", countQuery = "select count(*) from BasicTypeData")
    Page<WithDoubleValue> findAllDoubleValuesToPage(Pageable pageable);

    @Query(value = "select id, doubleValue from BasicTypeData", countQuery = "select count(*) from BasicTypeData")
    Slice<WithDoubleValue> findAllDoubleValuesToSlice(Pageable pageable);
}
