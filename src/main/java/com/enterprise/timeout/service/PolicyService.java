package com.enterprise.timeout.service;

import com.enterprise.timeout.audit.AuditService;
import com.enterprise.timeout.model.AuditAction;
import com.enterprise.timeout.model.PolicyLevel;
import com.enterprise.timeout.model.TimeoutPolicy;
import com.enterprise.timeout.repository.TimeoutPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final TimeoutPolicyRepository policyRepository;
    private final AuditService auditService;

    public Page<TimeoutPolicy> list(String teamId, PolicyLevel level, Boolean enabled, Pageable pageable) {
        return policyRepository.findByFilters(teamId, level, enabled, pageable);
    }

    public List<TimeoutPolicy> listAllEnabled() {
        return policyRepository.findByEnabledTrue();
    }

    public Optional<TimeoutPolicy> getById(String id) {
        return policyRepository.findById(id);
    }

    public TimeoutPolicy create(TimeoutPolicy policy) {
        TimeoutPolicy saved = policyRepository.save(policy);
        auditService.log(AuditAction.POLICY_CREATED, "user",
                "policy", saved.getId(),
                String.format("Created policy '%s' for team '%s'", saved.getName(), saved.getTeamId()));
        return saved;
    }

    public TimeoutPolicy update(String id, TimeoutPolicy updated) {
        TimeoutPolicy existing = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + id));

        existing.setName(updated.getName());
        existing.setLevel(updated.getLevel());
        existing.setTargetId(updated.getTargetId());
        existing.setTeamId(updated.getTeamId());
        existing.setTimeoutMinutes(updated.getTimeoutMinutes());
        existing.setAction(updated.getAction());
        existing.setAlertChannels(updated.getAlertChannels());
        existing.setEscalationMinutes(updated.getEscalationMinutes());
        existing.setEscalationContacts(updated.getEscalationContacts());
        existing.setEnabled(updated.isEnabled());

        TimeoutPolicy saved = policyRepository.save(existing);
        auditService.log(AuditAction.POLICY_UPDATED, "user",
                "policy", saved.getId(),
                String.format("Updated policy '%s'", saved.getName()));
        return saved;
    }

    public void delete(String id) {
        TimeoutPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + id));
        policyRepository.delete(policy);
        auditService.log(AuditAction.POLICY_DELETED, "user",
                "policy", id,
                String.format("Deleted policy '%s'", policy.getName()));
    }
}
