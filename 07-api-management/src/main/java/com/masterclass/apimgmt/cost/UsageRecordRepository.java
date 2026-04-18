package com.masterclass.apimgmt.cost;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

    List<UsageRecord> findByUserIdOrderByTimestampDesc(String userId);

    @Query("""
            SELECT new com.masterclass.apimgmt.cost.CostTracker$UserUsageSummary(
                u.userId,
                COUNT(u),
                SUM(u.promptTokens),
                SUM(u.completionTokens),
                SUM(u.estimatedCostUsd)
            )
            FROM UsageRecord u WHERE u.userId = :userId GROUP BY u.userId
            """)
    CostTracker.UserUsageSummary summarizeForUser(String userId);
}
