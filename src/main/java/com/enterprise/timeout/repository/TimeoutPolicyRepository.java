package com.enterprise.timeout.repository;

import com.enterprise.timeout.model.PolicyLevel;
import com.enterprise.timeout.model.TimeoutPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeoutPolicyRepository extends JpaRepository<TimeoutPolicy, String> {

    List<TimeoutPolicy> findByEnabledTrue();

    List<TimeoutPolicy> findByTeamId(String teamId);

    List<TimeoutPolicy> findByLevel(PolicyLevel level);

    Optional<TimeoutPolicy> findByLevelAndTargetId(PolicyLevel level, String targetId);

    @Query("SELECT p FROM TimeoutPolicy p WHERE " +
            "(:teamId IS NULL OR p.teamId = :teamId) AND " +
            "(:level IS NULL OR p.level = :level) AND " +
            "(:enabled IS NULL OR p.enabled = :enabled)")
    Page<TimeoutPolicy> findByFilters(
            @Param("teamId") String teamId,
            @Param("level") PolicyLevel level,
            @Param("enabled") Boolean enabled,
            Pageable pageable);
}
