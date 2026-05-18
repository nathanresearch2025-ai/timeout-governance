package com.enterprise.timeout.engine;

import com.enterprise.timeout.model.PolicyLevel;
import com.enterprise.timeout.model.TimeoutPolicy;
import com.enterprise.timeout.repository.TimeoutPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabasePolicyProvider implements PolicyProvider {

    private final TimeoutPolicyRepository policyRepository;

    @Override
    public List<TimeoutPolicy> loadAllPolicies() {
        return policyRepository.findByEnabledTrue();
    }

    @Override
    public Optional<TimeoutPolicy> findPolicy(PolicyLevel level, String targetId, String teamId) {
        return policyRepository.findByEnabledTrue().stream()
                .filter(p -> p.getLevel() == level)
                .filter(p -> matchesTarget(p, level, targetId, teamId))
                .findFirst();
    }

    @Override
    public void reload() {
        log.info("Database policy provider - no reload needed");
    }

    private boolean matchesTarget(TimeoutPolicy policy, PolicyLevel level, String targetId, String teamId) {
        return switch (level) {
            case WORKFLOW -> policy.getTargetId() != null && policy.getTargetId().equals(targetId);
            case TASK -> policy.getTargetId() != null && policy.getTargetId().equals(targetId);
            case TEAM -> policy.getTeamId() != null && policy.getTeamId().equals(teamId);
        };
    }
}
