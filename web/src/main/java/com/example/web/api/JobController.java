// web/src/main/java/com/example/web/api/JobController.java
package com.example.web.api;

import com.example.common.dto.JobHistoryResponse;
import com.example.common.dto.JobResponse;
import com.example.common.dto.RescheduleRequest;
import com.example.common.dto.ScheduleRequest;
import com.example.common.dto.JobDetailsResponse;
import com.example.scheduler.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService service;
    public JobController(JobService service) { this.service = service; }

    // --- needed for Job Type dropdown
    @GetMapping("/types")
    public ResponseEntity<List<String>> types() {
        return ResponseEntity.ok(service.supportedTypes());
    }

    // --- needed for Job selector dropdown
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, String>>> list() {
        return ResponseEntity.ok(service.listJobsSimple());
    }

    @PostMapping
    public ResponseEntity<JobResponse> schedule(@Valid @RequestBody ScheduleRequest req) throws Exception {
        return ResponseEntity.ok(service.schedule(req));
    }

    @PutMapping("/{group}/{name}")
    public ResponseEntity<JobResponse> reschedule(@PathVariable String group,
                                                  @PathVariable String name,
                                                  @RequestBody RescheduleRequest req) throws Exception {
        return ResponseEntity.ok(service.reschedule(name, group, req));
    }

    @PostMapping("/{group}/{name}/pause")
    public ResponseEntity<JobResponse> pause(@PathVariable String group, @PathVariable String name) throws Exception {
        return ResponseEntity.ok(service.stop(name, group));
    }

    @PostMapping("/{group}/{name}/resume")
    public ResponseEntity<JobResponse> resume(@PathVariable String group, @PathVariable String name) throws Exception {
        return ResponseEntity.ok(service.resume(name, group));
    }

    @GetMapping("/{jobId}/history")
    public ResponseEntity<List<JobHistoryResponse>> history(@PathVariable String jobId) {
        return ResponseEntity.ok(service.history(jobId));
    }

    @GetMapping("/{group}/{name}/history")
    public ResponseEntity<List<JobHistoryResponse>> historyByNameGroup(@PathVariable String group,
                                                                       @PathVariable String name) {
        return ResponseEntity.ok(service.historyByNameGroup(name, group));
    }

    @GetMapping("/{group}/{name}")
    public ResponseEntity<JobDetailsResponse> getJob(@PathVariable String group,
                                                     @PathVariable String name) throws Exception {
        return ResponseEntity.ok(service.details(name, group));
    }
}
