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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("EmployeeDeltaServiceImpl Tests")
class EmployeeDeltaServiceImplTest {

    @Mock
    private EmployeeIngestBatchRepository batchRepository;

    @Mock
    private EmployeeSnapshotRepository snapshotRepository;

    @Mock
    private EmployeeDeltaRepository deltaRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<EmployeeIngestBatch> batchCaptor;

    @Captor
    private ArgumentCaptor<List<EmployeeSnapshot>> snapshotsCaptor;

    @Captor
    private ArgumentCaptor<List<EmployeeDelta>> deltasCaptor;

    private EmployeeDeltaServiceImpl deltaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        deltaService = new EmployeeDeltaServiceImpl(
                batchRepository,
                snapshotRepository,
                deltaRepository,
                objectMapper
        );
    }

    @Nested
    @DisplayName("Ingest Batch Management Tests")
    class IngestBatchManagementTests {

        @Test
        @DisplayName("Should create ingest batch successfully")
        void shouldCreateIngestBatchSuccessfully() {
            // Arrange
            String batchId = "batch-001";
            String csvFileName = "employees.csv";
            EmployeeIngestBatch expectedBatch = createMockBatch(batchId, csvFileName);
            
            when(batchRepository.save(any(EmployeeIngestBatch.class))).thenReturn(expectedBatch);

            // Act
            EmployeeIngestBatch result = deltaService.createIngestBatch(batchId, csvFileName);

            // Assert
            assertNotNull(result);
            assertEquals(batchId, result.getBatchId());
            assertEquals(csvFileName, result.getCsvFileName());
            assertEquals(EmployeeIngestBatch.IngestStatus.PROCESSING, result.getStatus());
            
            verify(batchRepository).save(batchCaptor.capture());
            EmployeeIngestBatch capturedBatch = batchCaptor.getValue();
            assertEquals(batchId, capturedBatch.getBatchId());
            assertEquals(csvFileName, capturedBatch.getCsvFileName());
            assertEquals(EmployeeIngestBatch.IngestStatus.PROCESSING, capturedBatch.getStatus());
            assertNotNull(capturedBatch.getIngestDate());
        }

        @Test
        @DisplayName("Should update existing ingest batch successfully")
        void shouldUpdateExistingIngestBatchSuccessfully() {
            // Arrange
            String batchId = "batch-001";
            EmployeeIngestBatch existingBatch = createMockBatch(batchId, "test.csv");
            
            when(batchRepository.findByBatchId(batchId)).thenReturn(Optional.of(existingBatch));
            when(batchRepository.save(any(EmployeeIngestBatch.class))).thenReturn(existingBatch);

            // Act
            deltaService.updateIngestBatch(
                batchId, 
                EmployeeIngestBatch.IngestStatus.COMPLETED,
                100, 10, 5, null
            );

            // Assert
            verify(batchRepository).findByBatchId(batchId);
            verify(batchRepository).save(batchCaptor.capture());
            
            EmployeeIngestBatch updatedBatch = batchCaptor.getValue();
            assertEquals(EmployeeIngestBatch.IngestStatus.COMPLETED, updatedBatch.getStatus());
            assertEquals(100, updatedBatch.getTotalRecords());
            assertEquals(10, updatedBatch.getNewRecords());
            assertEquals(5, updatedBatch.getUpdatedRecords());
            assertNull(updatedBatch.getErrorMessage());
        }

        @Test
        @DisplayName("Should handle update of non-existent batch gracefully")
        void shouldHandleUpdateOfNonExistentBatchGracefully() {
            // Arrange
            String batchId = "non-existent-batch";
            when(batchRepository.findByBatchId(batchId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDoesNotThrow(() -> {
                deltaService.updateIngestBatch(
                    batchId, 
                    EmployeeIngestBatch.IngestStatus.FAILED,
                    0, 0, 0, "Error message"
                );
            });

            verify(batchRepository).findByBatchId(batchId);
            verify(batchRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should get most recent batch successfully")
        void shouldGetMostRecentBatchSuccessfully() {
            // Arrange
            EmployeeIngestBatch recentBatch = createMockBatch("batch-002", "recent.csv");
            when(batchRepository.findMostRecentCompletedBatch()).thenReturn(List.of(recentBatch));

            // Act
            EmployeeIngestBatch result = deltaService.getMostRecentBatch();

            // Assert
            assertNotNull(result);
            assertEquals("batch-002", result.getBatchId());
            verify(batchRepository).findMostRecentCompletedBatch();
        }

        @Test
        @DisplayName("Should return null when no recent batch exists")
        void shouldReturnNullWhenNoRecentBatchExists() {
            // Arrange
            when(batchRepository.findMostRecentCompletedBatch()).thenReturn(List.of());

            // Act
            EmployeeIngestBatch result = deltaService.getMostRecentBatch();

            // Assert
            assertNull(result);
            verify(batchRepository).findMostRecentCompletedBatch();
        }
    }

    @Nested
    @DisplayName("Employee Snapshot Tests")
    class EmployeeSnapshotTests {

        @Test
        @DisplayName("Should create snapshots for multiple employees")
        void shouldCreateSnapshotsForMultipleEmployees() {
            // Arrange
            String batchId = "batch-001";
            List<Employee> employees = List.of(
                createMockEmployee(1L, "Alice", 30),
                createMockEmployee(2L, "Bob", 25)
            );

            when(snapshotRepository.saveAll(any())).thenReturn(List.of());

            // Act
            deltaService.createEmployeeSnapshots(employees, batchId);

            // Assert
            verify(snapshotRepository).saveAll(snapshotsCaptor.capture());
            List<EmployeeSnapshot> capturedSnapshots = snapshotsCaptor.getValue();
            
            assertEquals(2, capturedSnapshots.size());
            assertEquals(1L, capturedSnapshots.get(0).getEmployeeId());
            assertEquals(2L, capturedSnapshots.get(1).getEmployeeId());
            assertEquals(batchId, capturedSnapshots.get(0).getBatchId());
            assertEquals(batchId, capturedSnapshots.get(1).getBatchId());
        }

        @Test
        @DisplayName("Should handle empty employee list")
        void shouldHandleEmptyEmployeeList() {
            // Arrange
            String batchId = "batch-001";
            List<Employee> employees = List.of();

            when(snapshotRepository.saveAll(any())).thenReturn(List.of());

            // Act
            deltaService.createEmployeeSnapshots(employees, batchId);

            // Assert
            verify(snapshotRepository).saveAll(snapshotsCaptor.capture());
            List<EmployeeSnapshot> capturedSnapshots = snapshotsCaptor.getValue();
            assertTrue(capturedSnapshots.isEmpty());
        }

        @Test
        @DisplayName("Should create single employee snapshot")
        void shouldCreateSingleEmployeeSnapshot() {
            // Arrange
            String batchId = "batch-001";
            Employee employee = createMockEmployee(1L, "Charlie", 35);
            List<Employee> employees = List.of(employee);

            when(snapshotRepository.saveAll(any())).thenReturn(List.of());

            // Act
            deltaService.createEmployeeSnapshots(employees, batchId);

            // Assert
            verify(snapshotRepository).saveAll(snapshotsCaptor.capture());
            List<EmployeeSnapshot> capturedSnapshots = snapshotsCaptor.getValue();
            
            assertEquals(1, capturedSnapshots.size());
            EmployeeSnapshot snapshot = capturedSnapshots.get(0);
            assertEquals(1L, snapshot.getEmployeeId());
            assertEquals("Charlie", snapshot.getName());
            assertEquals(35, snapshot.getAge());
            assertEquals(batchId, snapshot.getBatchId());
        }
    }

    @Nested
    @DisplayName("Delta Detection Tests")
    class DeltaDetectionTests {

        @Test
        @DisplayName("Should detect NEW employees when no previous batch exists")
        void shouldDetectNewEmployeesWhenNoPreviousBatchExists() {
            // Arrange
            String currentBatchId = "batch-001";
            List<EmployeeSnapshot> currentSnapshots = List.of(
                createMockSnapshot(1L, "Alice", 30, currentBatchId),
                createMockSnapshot(2L, "Bob", 25, currentBatchId)
            );

            when(snapshotRepository.findByBatchId(currentBatchId)).thenReturn(currentSnapshots);
            when(batchRepository.findByBatchId(currentBatchId)).thenReturn(Optional.empty());
            when(deltaRepository.saveAll(any())).thenReturn(List.of());

            // Act
            List<EmployeeDelta> result = deltaService.detectAndRecordDeltas(currentBatchId);

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(d -> d.getDeltaType() == EmployeeDelta.DeltaType.NEW));
            // The service saves deltas when it finds them (not empty)
            verify(deltaRepository).saveAll(any());
        }

        @Test
        @DisplayName("Should detect NEW, UPDATED, and DELETED employees")
        void shouldDetectNewUpdatedAndDeletedEmployees() throws JsonProcessingException {
            // Arrange
            String currentBatchId = "batch-002";
            String previousBatchId = "batch-001";
            
            // Current batch has: Alice (updated), Charlie (new)
            List<EmployeeSnapshot> currentSnapshots = List.of(
                createMockSnapshot(1L, "Alice Updated", 31, currentBatchId), // Updated
                createMockSnapshot(3L, "Charlie", 28, currentBatchId)        // New
            );
            
            // Previous batch had: Alice (original), Bob (deleted)
            List<EmployeeSnapshot> previousSnapshots = List.of(
                createMockSnapshot(1L, "Alice", 30, previousBatchId),        // Will be updated
                createMockSnapshot(2L, "Bob", 25, previousBatchId)           // Will be deleted
            );

            EmployeeIngestBatch previousBatch = createMockBatch(previousBatchId, "previous.csv");
            EmployeeIngestBatch currentBatch = createMockBatch(currentBatchId, "current.csv");

            when(snapshotRepository.findByBatchId(currentBatchId)).thenReturn(currentSnapshots);
            when(batchRepository.findByBatchId(currentBatchId)).thenReturn(Optional.of(currentBatch));
            when(batchRepository.findMostRecentCompletedBatchBefore(any()))
                .thenReturn(List.of(previousBatch));
            when(snapshotRepository.findByBatchId(previousBatchId)).thenReturn(previousSnapshots);
            when(objectMapper.writeValueAsString(any())).thenReturn("[\"name\",\"age\"]");
            when(deltaRepository.saveAll(any())).thenReturn(List.of());

            // Act
            List<EmployeeDelta> result = deltaService.detectAndRecordDeltas(currentBatchId);

            // Assert
            assertEquals(3, result.size());
            verify(deltaRepository).saveAll(deltasCaptor.capture());
            List<EmployeeDelta> capturedDeltas = deltasCaptor.getValue();
            
            assertEquals(3, capturedDeltas.size());
            
            // Verify we have one of each type
            Map<EmployeeDelta.DeltaType, Long> deltaTypeCounts = capturedDeltas.stream()
                .collect(Collectors.groupingBy(EmployeeDelta::getDeltaType, Collectors.counting()));
            
            assertEquals(1, deltaTypeCounts.get(EmployeeDelta.DeltaType.NEW));
            assertEquals(1, deltaTypeCounts.get(EmployeeDelta.DeltaType.UPDATED));
            assertEquals(1, deltaTypeCounts.get(EmployeeDelta.DeltaType.DELETED));
            
            // Verify specific deltas
            EmployeeDelta newDelta = capturedDeltas.stream()
                .filter(d -> d.getDeltaType() == EmployeeDelta.DeltaType.NEW)
                .findFirst().orElse(null);
            assertNotNull(newDelta);
            assertEquals(3L, newDelta.getEmployeeId());
            assertEquals("Charlie", newDelta.getCurrentName());

            EmployeeDelta updatedDelta = capturedDeltas.stream()
                .filter(d -> d.getDeltaType() == EmployeeDelta.DeltaType.UPDATED)
                .findFirst().orElse(null);
            assertNotNull(updatedDelta);
            assertEquals(1L, updatedDelta.getEmployeeId());
            assertEquals("Alice", updatedDelta.getPreviousName());
            assertEquals("Alice Updated", updatedDelta.getCurrentName());
            assertEquals(30, updatedDelta.getPreviousAge());
            assertEquals(31, updatedDelta.getCurrentAge());

            EmployeeDelta deletedDelta = capturedDeltas.stream()
                .filter(d -> d.getDeltaType() == EmployeeDelta.DeltaType.DELETED)
                .findFirst().orElse(null);
            assertNotNull(deletedDelta);
            assertEquals(2L, deletedDelta.getEmployeeId());
            assertEquals("Bob", deletedDelta.getPreviousName());
            assertNull(deletedDelta.getCurrentName());
        }

        @Test
        @DisplayName("Should not create delta for unchanged employee")
        void shouldNotCreateDeltaForUnchangedEmployee() {
            // Arrange
            String currentBatchId = "batch-002";
            String previousBatchId = "batch-001";
            
            // Same employee in both batches
            List<EmployeeSnapshot> currentSnapshots = List.of(
                createMockSnapshot(1L, "Alice", 30, currentBatchId)
            );
            List<EmployeeSnapshot> previousSnapshots = List.of(
                createMockSnapshot(1L, "Alice", 30, previousBatchId)
            );

            EmployeeIngestBatch previousBatch = createMockBatch(previousBatchId, "previous.csv");
            EmployeeIngestBatch currentBatch = createMockBatch(currentBatchId, "current.csv");

            when(snapshotRepository.findByBatchId(currentBatchId)).thenReturn(currentSnapshots);
            when(batchRepository.findByBatchId(currentBatchId)).thenReturn(Optional.of(currentBatch));
            when(batchRepository.findMostRecentCompletedBatchBefore(any()))
                .thenReturn(List.of(previousBatch));
            when(snapshotRepository.findByBatchId(previousBatchId)).thenReturn(previousSnapshots);

            // Act
            List<EmployeeDelta> result = deltaService.detectAndRecordDeltas(currentBatchId);

            // Assert
            assertTrue(result.isEmpty());
            // No deltas to save, so saveAll should not be called
            verify(deltaRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should handle JSON serialization failure gracefully")
        void shouldHandleJsonSerializationFailureGracefully() throws JsonProcessingException {
            // Arrange
            String currentBatchId = "batch-002";
            String previousBatchId = "batch-001";
            
            List<EmployeeSnapshot> currentSnapshots = List.of(
                createMockSnapshot(1L, "Alice Updated", 31, currentBatchId)
            );
            List<EmployeeSnapshot> previousSnapshots = List.of(
                createMockSnapshot(1L, "Alice", 30, previousBatchId)
            );

            EmployeeIngestBatch previousBatch = createMockBatch(previousBatchId, "previous.csv");
            EmployeeIngestBatch currentBatch = createMockBatch(currentBatchId, "current.csv");

            when(snapshotRepository.findByBatchId(currentBatchId)).thenReturn(currentSnapshots);
            when(batchRepository.findByBatchId(currentBatchId)).thenReturn(Optional.of(currentBatch));
            when(batchRepository.findMostRecentCompletedBatchBefore(any()))
                .thenReturn(List.of(previousBatch));
            when(snapshotRepository.findByBatchId(previousBatchId)).thenReturn(previousSnapshots);
            when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Test error") {});
            when(deltaRepository.saveAll(any())).thenReturn(List.of());

            // Act & Assert
            assertDoesNotThrow(() -> {
                List<EmployeeDelta> result = deltaService.detectAndRecordDeltas(currentBatchId);
                assertEquals(1, result.size());
            });

            verify(deltaRepository).saveAll(deltasCaptor.capture());
            List<EmployeeDelta> capturedDeltas = deltasCaptor.getValue();
            
            assertEquals(1, capturedDeltas.size());
            EmployeeDelta delta = capturedDeltas.get(0);
            // Should fall back to comma-separated string
            assertEquals("name,age", delta.getChangedFields());
        }
    }

    @Nested
    @DisplayName("Delta Retrieval Tests")
    class DeltaRetrievalTests {

        @Test
        @DisplayName("Should get all deltas for batch")
        void shouldGetAllDeltasForBatch() {
            // Arrange
            String batchId = "batch-001";
            List<EmployeeDelta> expectedDeltas = List.of(
                createMockDelta(1L, EmployeeDelta.DeltaType.NEW, batchId),
                createMockDelta(2L, EmployeeDelta.DeltaType.UPDATED, batchId)
            );
            
            when(deltaRepository.findByBatchId(batchId)).thenReturn(expectedDeltas);

            // Act
            List<EmployeeDelta> result = deltaService.getDeltasForBatch(batchId);

            // Assert
            assertEquals(2, result.size());
            assertEquals(expectedDeltas, result);
            verify(deltaRepository).findByBatchId(batchId);
        }

        @Test
        @DisplayName("Should get deltas by type for batch")
        void shouldGetDeltasByTypeForBatch() {
            // Arrange
            String batchId = "batch-001";
            EmployeeDelta.DeltaType deltaType = EmployeeDelta.DeltaType.NEW;
            List<EmployeeDelta> expectedDeltas = List.of(
                createMockDelta(1L, deltaType, batchId)
            );
            
            when(deltaRepository.findByBatchIdAndDeltaType(batchId, deltaType)).thenReturn(expectedDeltas);

            // Act
            List<EmployeeDelta> result = deltaService.getDeltasForBatch(batchId, deltaType);

            // Assert
            assertEquals(1, result.size());
            assertEquals(expectedDeltas, result);
            verify(deltaRepository).findByBatchIdAndDeltaType(batchId, deltaType);
        }

        @Test
        @DisplayName("Should get delta summary for batch")
        void shouldGetDeltaSummaryForBatch() {
            // Arrange
            String batchId = "batch-001";
            List<Object[]> counts = List.of(
                new Object[]{EmployeeDelta.DeltaType.NEW, 5L},
                new Object[]{EmployeeDelta.DeltaType.UPDATED, 3L},
                new Object[]{EmployeeDelta.DeltaType.DELETED, 2L}
            );
            
            when(deltaRepository.countDeltasByTypeForBatch(batchId)).thenReturn(counts);

            // Act
            EmployeeDeltaService.DeltaSummary result = deltaService.getDeltaSummary(batchId);

            // Assert
            assertNotNull(result);
            assertEquals(batchId, result.getBatchId());
            assertEquals(5, result.getNewEmployees());
            assertEquals(3, result.getUpdatedEmployees());
            assertEquals(2, result.getDeletedEmployees());
            assertEquals(10, result.getTotalDeltas());
            verify(deltaRepository).countDeltasByTypeForBatch(batchId);
        }

        @Test
        @DisplayName("Should handle empty delta summary")
        void shouldHandleEmptyDeltaSummary() {
            // Arrange
            String batchId = "batch-001";
            when(deltaRepository.countDeltasByTypeForBatch(batchId)).thenReturn(List.of());

            // Act
            EmployeeDeltaService.DeltaSummary result = deltaService.getDeltaSummary(batchId);

            // Assert
            assertNotNull(result);
            assertEquals(batchId, result.getBatchId());
            assertEquals(0, result.getNewEmployees());
            assertEquals(0, result.getUpdatedEmployees());
            assertEquals(0, result.getDeletedEmployees());
            assertEquals(0, result.getTotalDeltas());
        }
    }

    // Helper methods
    private EmployeeIngestBatch createMockBatch(String batchId, String fileName) {
        EmployeeIngestBatch batch = new EmployeeIngestBatch();
        batch.setBatchId(batchId);
        batch.setCsvFileName(fileName);
        batch.setIngestDate(LocalDateTime.now());
        batch.setStatus(EmployeeIngestBatch.IngestStatus.PROCESSING);
        return batch;
    }

    private Employee createMockEmployee(Long id, String name, Integer age) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(name);
        employee.setAge(age);
        employee.setStatus("ACTIVE");
        employee.setDob(new Date());
        return employee;
    }

    private EmployeeSnapshot createMockSnapshot(Long employeeId, String name, Integer age, String batchId) {
        EmployeeSnapshot snapshot = new EmployeeSnapshot();
        snapshot.setEmployeeId(employeeId);
        snapshot.setName(name);
        snapshot.setAge(age);
        snapshot.setStatus("ACTIVE");
        snapshot.setBatchId(batchId);
        snapshot.setSnapshotDate(LocalDateTime.now());
        snapshot.setDob(new Date());
        return snapshot;
    }

    private EmployeeDelta createMockDelta(Long employeeId, EmployeeDelta.DeltaType deltaType, String batchId) {
        EmployeeDelta delta = new EmployeeDelta();
        delta.setId(1L);
        delta.setEmployeeId(employeeId);
        delta.setDeltaType(deltaType);
        delta.setBatchId(batchId);
        delta.setDetectedDate(LocalDateTime.now());
        return delta;
    }
}