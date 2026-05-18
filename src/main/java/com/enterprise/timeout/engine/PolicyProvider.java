package com.enterprise.timeout.engine;

import com.enterprise.timeout.model.PolicyLevel;
import com.enterprise.timeout.model.TimeoutPolicy;
import java.util.List;
import java.util.Optional;

public interface PolicyProvider {

    List<TimeoutPolicy> loadAllPolicies();

    Optional<TimeoutPolicy> findPolicy(PolicyLevel level, String targetId, String teamId);

    void reload();
}
