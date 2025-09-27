package com.example.employee.service.impl;

import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeDelta;
import com.example.employee.entity.EmployeeIngestBatch;
import com.example.employee.entity.EmployeeSnapshot;
import com.example.employee.repo.EmployeeDeltaRepository;
import com.example.employee.repo.EmployeeIngestBatchRepository;
import com.example.employee.repo.EmployeeSnapshotRepository;
import com.example.employee.service.EmployeeDeltaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmployeeDeltaServiceImpl implements EmployeeDeltaService {
    
    private static final Logger log = LoggerFactory.getLogger(EmployeeDeltaServiceImpl.class);
    
    private final EmployeeIngestBatchRepository batchRepository;
    private final EmployeeSnapshotRepository snapshotRepository;
    private final EmployeeDeltaRepository deltaRepository;
    private final ObjectMapper objectMapper;
    
    public EmployeeDeltaServiceImpl(
            EmployeeIngestBatchRepository batchRepository,
            EmployeeSnapshotRepository snapshotRepository,
            EmployeeDeltaRepository deltaRepository,
            ObjectMapper objectMapper) {
        this.batchRepository = batchRepository;
        this.snapshotRepository = snapshotRepository;
        this.deltaRepository = deltaRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public EmployeeIngestBatch createIngestBatch(String batchId, String csvFileName) {
        log.info("Creating ingest batch: {} for file: {}", batchId, csvFileName);
        
        EmployeeIngestBatch batch = new EmployeeIngestBatch();
        batch.setBatchId(batchId);
        batch.setCsvFileName(csvFileName);
        batch.setIngestDate(LocalDateTime.now());
        batch.setStatus(EmployeeIngestBatch.IngestStatus.PROCESSING);
        
        return batchRepository.save(batch);
    }
    
    @Override
    public void updateIngestBatch(String batchId, EmployeeIngestBatch.IngestStatus status, 
                                 Integer totalRecords, Integer newRecords, Integer updatedRecords, 
                                 String errorMessage) {
        log.info("Updating ingest batch: {} with status: {}", batchId, status);
        
        Optional<EmployeeIngestBatch> batchOpt = batchRepository.findByBatchId(batchId);
        if (batchOpt.isPresent()) {
            EmployeeIngestBatch batch = batchOpt.get();
            batch.setStatus(status);
            batch.setTotalRecords(totalRecords);
            batch.setNewRecords(newRecords);
            batch.setUpdatedRecords(updatedRecords);
            batch.setErrorMessage(errorMessage);
            batchRepository.save(batch);
        } else {
            log.warn("Batch not found for update: {}", batchId);
        }
    }
    
    @Override
    public void createEmployeeSnapshots(List<Employee> employees, String batchId) {
        log.info("Creating {} employee snapshots for batch: {}", employees.size(), batchId);
        
        List<EmployeeSnapshot> snapshots = employees.stream()
                .map(emp -> EmployeeSnapshot.fromEmployee(emp, batchId))
                .collect(Collectors.toList());
        
        snapshotRepository.saveAll(snapshots);
        log.info("Saved {} employee snapshots for batch: {}", snapshots.size(), batchId);
    }
    
    @Override
    public List<EmployeeDelta> detectAndRecordDeltas(String currentBatchId) {
        log.info("Detecting deltas for batch: {}", currentBatchId);
        
        // Get current batch snapshots
        List<EmployeeSnapshot> currentSnapshots = snapshotRepository.findByBatchId(currentBatchId);
        Map<Long, EmployeeSnapshot> currentEmployeeMap = currentSnapshots.stream()
                .collect(Collectors.toMap(EmployeeSnapshot::getEmployeeId, s -> s));
        
        // Get previous batch
        EmployeeIngestBatch previousBatch = getPreviousBatch(currentBatchId);
        if (previousBatch == null) {
            log.info("No previous batch found. All {} employees will be marked as NEW.", currentSnapshots.size());
            List<EmployeeDelta> newDeltas = createNewEmployeeDeltas(currentSnapshots, currentBatchId, null);
            if (!newDeltas.isEmpty()) {
                deltaRepository.saveAll(newDeltas);
                log.info("Detected and saved {} deltas for batch: {} (NEW: {}, UPDATED: 0, DELETED: 0)",
                        newDeltas.size(), currentBatchId, newDeltas.size());
            }
            return newDeltas;
        }
        
        log.info("Comparing with previous batch: {}", previousBatch.getBatchId());
        
        // Get previous batch snapshots
        List<EmployeeSnapshot> previousSnapshots = snapshotRepository.findByBatchId(previousBatch.getBatchId());
        Map<Long, EmployeeSnapshot> previousEmployeeMap = previousSnapshots.stream()
                .collect(Collectors.toMap(EmployeeSnapshot::getEmployeeId, s -> s));
        
        List<EmployeeDelta> deltas = new ArrayList<>();
        
        // Find NEW employees (in current but not in previous)
        Set<Long> newEmployeeIds = new HashSet<>(currentEmployeeMap.keySet());
        newEmployeeIds.removeAll(previousEmployeeMap.keySet());
        for (Long employeeId : newEmployeeIds) {
            EmployeeSnapshot current = currentEmployeeMap.get(employeeId);
            EmployeeDelta delta = createNewEmployeeDelta(current, currentBatchId, previousBatch.getBatchId());
            deltas.add(delta);
        }
        
        // Find DELETED employees (in previous but not in current)
        Set<Long> deletedEmployeeIds = new HashSet<>(previousEmployeeMap.keySet());
        deletedEmployeeIds.removeAll(currentEmployeeMap.keySet());
        for (Long employeeId : deletedEmployeeIds) {
            EmployeeSnapshot previous = previousEmployeeMap.get(employeeId);
            EmployeeDelta delta = createDeletedEmployeeDelta(previous, currentBatchId, previousBatch.getBatchId());
            deltas.add(delta);
        }
        
        // Find UPDATED employees (in both, but with changes)
        Set<Long> commonEmployeeIds = new HashSet<>(currentEmployeeMap.keySet());
        commonEmployeeIds.retainAll(previousEmployeeMap.keySet());
        for (Long employeeId : commonEmployeeIds) {
            EmployeeSnapshot current = currentEmployeeMap.get(employeeId);
            EmployeeSnapshot previous = previousEmployeeMap.get(employeeId);
            EmployeeDelta delta = detectEmployeeChanges(current, previous, currentBatchId, previousBatch.getBatchId());
            if (delta != null) {
                deltas.add(delta);
            }
        }
        
        // Save all deltas
        if (!deltas.isEmpty()) {
            deltaRepository.saveAll(deltas);
            log.info("Detected and saved {} deltas for batch: {} (NEW: {}, UPDATED: {}, DELETED: {})",
                    deltas.size(), currentBatchId,
                    deltas.stream().mapToInt(d -> d.getDeltaType() == EmployeeDelta.DeltaType.NEW ? 1 : 0).sum(),
                    deltas.stream().mapToInt(d -> d.getDeltaType() == EmployeeDelta.DeltaType.UPDATED ? 1 : 0).sum(),
                    deltas.stream().mapToInt(d -> d.getDeltaType() == EmployeeDelta.DeltaType.DELETED ? 1 : 0).sum());
        } else {
            log.info("No deltas detected for batch: {}", currentBatchId);
        }
        
        return deltas;
    }
    
    private List<EmployeeDelta> createNewEmployeeDeltas(List<EmployeeSnapshot> snapshots, String currentBatchId, String previousBatchId) {
        return snapshots.stream()
                .map(snapshot -> createNewEmployeeDelta(snapshot, currentBatchId, previousBatchId))
                .collect(Collectors.toList());
    }
    
    private EmployeeDelta createNewEmployeeDelta(EmployeeSnapshot current, String currentBatchId, String previousBatchId) {
        EmployeeDelta delta = new EmployeeDelta();
        delta.setEmployeeId(current.getEmployeeId());
        delta.setBatchId(currentBatchId);
        delta.setPreviousBatchId(previousBatchId);
        delta.setDeltaType(EmployeeDelta.DeltaType.NEW);
        delta.setDetectedDate(LocalDateTime.now());
        
        // Set current values
        delta.setCurrentName(current.getName());
        delta.setCurrentAge(current.getAge());
        delta.setCurrentStatus(current.getStatus());
        delta.setCurrentDob(current.getDob());
        
        delta.setChangeSummary("New employee added: " + current.getName() + " (ID: " + current.getEmployeeId() + ")");
        
        return delta;
    }
    
    private EmployeeDelta createDeletedEmployeeDelta(EmployeeSnapshot previous, String currentBatchId, String previousBatchId) {
        EmployeeDelta delta = new EmployeeDelta();
        delta.setEmployeeId(previous.getEmployeeId());
        delta.setBatchId(currentBatchId);
        delta.setPreviousBatchId(previousBatchId);
        delta.setDeltaType(EmployeeDelta.DeltaType.DELETED);
        delta.setDetectedDate(LocalDateTime.now());
        
        // Set previous values
        delta.setPreviousName(previous.getName());
        delta.setPreviousAge(previous.getAge());
        delta.setPreviousStatus(previous.getStatus());
        delta.setPreviousDob(previous.getDob());
        
        delta.setChangeSummary("Employee deleted: " + previous.getName() + " (ID: " + previous.getEmployeeId() + ")");
        
        return delta;
    }
    
    private EmployeeDelta detectEmployeeChanges(EmployeeSnapshot current, EmployeeSnapshot previous, 
                                              String currentBatchId, String previousBatchId) {
        List<String> changedFields = new ArrayList<>();
        List<String> changeSummaryParts = new ArrayList<>();
        
        // Compare each field
        if (!Objects.equals(current.getName(), previous.getName())) {
            changedFields.add("name");
            changeSummaryParts.add(String.format("name: '%s' -> '%s'", previous.getName(), current.getName()));
        }
        
        if (!Objects.equals(current.getAge(), previous.getAge())) {
            changedFields.add("age");
            changeSummaryParts.add(String.format("age: %s -> %s", previous.getAge(), current.getAge()));
        }
        
        if (!Objects.equals(current.getStatus(), previous.getStatus())) {
            changedFields.add("status");
            changeSummaryParts.add(String.format("status: '%s' -> '%s'", previous.getStatus(), current.getStatus()));
        }
        
        if (!Objects.equals(current.getDob(), previous.getDob())) {
            changedFields.add("dob");
            changeSummaryParts.add(String.format("dob: %s -> %s", previous.getDob(), current.getDob()));
        }
        
        // If no changes detected, return null
        if (changedFields.isEmpty()) {
            return null;
        }
        
        // Create delta record for updated employee
        EmployeeDelta delta = new EmployeeDelta();
        delta.setEmployeeId(current.getEmployeeId());
        delta.setBatchId(currentBatchId);
        delta.setPreviousBatchId(previousBatchId);
        delta.setDeltaType(EmployeeDelta.DeltaType.UPDATED);
        delta.setDetectedDate(LocalDateTime.now());
        
        // Set previous values
        delta.setPreviousName(previous.getName());
        delta.setPreviousAge(previous.getAge());
        delta.setPreviousStatus(previous.getStatus());
        delta.setPreviousDob(previous.getDob());
        
        // Set current values
        delta.setCurrentName(current.getName());
        delta.setCurrentAge(current.getAge());
        delta.setCurrentStatus(current.getStatus());
        delta.setCurrentDob(current.getDob());
        
        // Set change metadata
        try {
            delta.setChangedFields(objectMapper.writeValueAsString(changedFields));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize changed fields: {}", changedFields, e);
            delta.setChangedFields(String.join(",", changedFields));
        }
        
        delta.setChangeSummary(String.format("Employee updated: %s (ID: %d) - %s", 
                current.getName(), current.getEmployeeId(), String.join(", ", changeSummaryParts)));
        
        return delta;
    }
    
    private EmployeeIngestBatch getPreviousBatch(String currentBatchId) {
        // Get the current batch to find its ingest date
        Optional<EmployeeIngestBatch> currentBatchOpt = batchRepository.findByBatchId(currentBatchId);
        if (currentBatchOpt.isEmpty()) {
            return null;
        }
        
        LocalDateTime currentIngestDate = currentBatchOpt.get().getIngestDate();
        
        // Find the most recent completed batch before the current one
        List<EmployeeIngestBatch> previousBatches = batchRepository.findMostRecentCompletedBatchBefore(currentIngestDate);
        return previousBatches.isEmpty() ? null : previousBatches.get(0);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDelta> getDeltasForBatch(String batchId) {
        return deltaRepository.findByBatchId(batchId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDelta> getDeltasForBatch(String batchId, EmployeeDelta.DeltaType deltaType) {
        return deltaRepository.findByBatchIdAndDeltaType(batchId, deltaType);
    }
    
    @Override
    @Transactional(readOnly = true)
    public EmployeeIngestBatch getMostRecentBatch() {
        List<EmployeeIngestBatch> batches = batchRepository.findMostRecentCompletedBatch();
        return batches.isEmpty() ? null : batches.get(0);
    }
    
    @Override
    @Transactional(readOnly = true)
    public DeltaSummary getDeltaSummary(String batchId) {
        List<Object[]> counts = deltaRepository.countDeltasByTypeForBatch(batchId);
        
        int newEmployees = 0;
        int updatedEmployees = 0;
        int deletedEmployees = 0;
        
        for (Object[] count : counts) {
            EmployeeDelta.DeltaType type = (EmployeeDelta.DeltaType) count[0];
            Long countValue = (Long) count[1];
            
            switch (type) {
                case NEW -> newEmployees = countValue.intValue();
                case UPDATED -> updatedEmployees = countValue.intValue();
                case DELETED -> deletedEmployees = countValue.intValue();
            }
        }
        
        return new DeltaSummary(batchId, newEmployees, updatedEmployees, deletedEmployees);
    }
}