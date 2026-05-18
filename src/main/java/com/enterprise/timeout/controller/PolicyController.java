package com.enterprise.timeout.controller;

import com.enterprise.timeout.model.PolicyLevel;
import com.enterprise.timeout.model.TimeoutPolicy;
import com.enterprise.timeout.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public Page<TimeoutPolicy> list(
            @RequestParam(required = false) String teamId,
            @RequestParam(required = false) PolicyLevel level,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return policyService.list(teamId, level, enabled,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeoutPolicy> getById(@PathVariable String id) {
        return policyService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeoutPolicy create(@Valid @RequestBody TimeoutPolicy policy) {
        return policyService.create(policy);
    }

    @PutMapping("/{id}")
    public TimeoutPolicy update(@PathVariable String id, @Valid @RequestBody TimeoutPolicy policy) {
        return policyService.update(id, policy);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        policyService.delete(id);
    }
}
