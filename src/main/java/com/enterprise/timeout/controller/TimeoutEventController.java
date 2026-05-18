package com.enterprise.timeout.controller;

import com.enterprise.timeout.model.TimeoutEvent;
import com.enterprise.timeout.repository.TimeoutEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/timeout-events")
@RequiredArgsConstructor
public class TimeoutEventController {

    private final TimeoutEventRepository timeoutEventRepository;

    @GetMapping
    public Page<TimeoutEvent> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return timeoutEventRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "detectedAt")));
    }

    @GetMapping("/unresolved")
    public List<TimeoutEvent> listUnresolved() {
        return timeoutEventRepository.findByResolvedAtIsNullAndEscalatedFalse();
    }
}
