package com.enterprise.timeout.controller;

import com.enterprise.timeout.model.PolicyLevel;
import com.enterprise.timeout.model.TimeoutPolicy;
import com.enterprise.timeout.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workflow-policies")
@RequiredArgsConstructor
public class WorkflowPolicyController {

    private final PolicyService policyService;

    @GetMapping
    public List<TimeoutPolicy> listWorkflowPolicies(
            @RequestParam(required = false) String teamId,
            @RequestParam(required = false) Boolean enabled) {
        return policyService.listByLevelAndFilters(PolicyLevel.WORKFLOW, teamId, enabled);
    }

    @GetMapping("/target/{targetId}")
    public ResponseEntity<TimeoutPolicy> getByTargetId(@PathVariable String targetId) {
        return policyService.getByLevelAndTarget(PolicyLevel.WORKFLOW, targetId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeoutPolicy createWorkflowPolicy(@Valid @RequestBody TimeoutPolicy policy) {
        policy.setLevel(PolicyLevel.WORKFLOW);
        return policyService.create(policy);
    }

    @PutMapping("/{id}")
    public TimeoutPolicy updateWorkflowPolicy(@PathVariable String id, @Valid @RequestBody TimeoutPolicy policy) {
        policy.setLevel(PolicyLevel.WORKFLOW);
        return policyService.update(id, policy);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkflowPolicy(@PathVariable String id) {
        policyService.delete(id);
    }

    @PatchMapping("/{id}/toggle")
    public TimeoutPolicy toggleEnabled(@PathVariable String id) {
        return policyService.toggleEnabled(id);
    }
}
