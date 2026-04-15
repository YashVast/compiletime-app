package com.compiletime.infrastructure;

import com.compiletime.domain.XpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface XpRecordRepository extends JpaRepository<XpRecord, Long> {

    @Query("SELECT COALESCE(SUM(x.xpAwarded), 0) FROM XpRecord x")
    long sumXp();
}
