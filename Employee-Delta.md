# Employee Delta Detection System

## Overview

This document describes the comprehensive delta detection system implemented for the platform-scheduler project. The system automatically tracks changes between CSV ingestion cycles, identifying new, updated, and deleted employee records.

## Features

### Core Capabilities
- **NEW Records Detection**: Identifies employees that appear in current batch but not in previous batch
- **UPDATED Records Detection**: Detects changes in employee metadata (name, age, status, date of birth)
- **DELETED Records Detection**: Finds employees present in previous batch but missing in current batch
- **Automated Reporting**: Generates CSV reports of detected changes via Quartz scheduler
- **REST API**: Provides endpoints for querying delta information
- **Historical Tracking**: Maintains snapshots of all employee data for comparison

## System Architecture

### Database Schema (V8 Migration)

#### Employee Ingest Batch Table
```sql
CREATE TABLE employee_ingest_batch (
    id UUID PRIMARY KEY,
    ingest_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    total_records INTEGER DEFAULT 0,
    new_records INTEGER DEFAULT 0,
    updated_records INTEGER DEFAULT 0,
    deleted_records INTEGER DEFAULT 0,
    error_message TEXT,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Employee Snapshot Table
```sql
CREATE TABLE employee_snapshot (
    id BIGSERIAL PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES employee_ingest_batch(id),
    employee_id BIGINT,
    name VARCHAR(255),
    age INTEGER,
    status VARCHAR(50),
    dob DATE,
    snapshot_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### Employee Delta Table
```sql
CREATE TABLE employee_delta (
    id BIGSERIAL PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES employee_ingest_batch(id),
    employee_id BIGINT,
    delta_type VARCHAR(20) NOT NULL CHECK (delta_type IN ('NEW', 'UPDATED', 'DELETED')),
    previous_values JSONB,
    current_values JSONB,
    changed_fields TEXT[],
    summary TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### Indexes for Performance
```sql
CREATE INDEX idx_employee_snapshot_batch_id ON employee_snapshot(batch_id);
CREATE INDEX idx_employee_snapshot_employee_id ON employee_snapshot(employee_id);
CREATE INDEX idx_employee_delta_batch_id ON employee_delta(batch_id);
CREATE INDEX idx_employee_delta_type ON employee_delta(delta_type);
CREATE INDEX idx_employee_delta_created_date ON employee_delta(created_date);
```

## Implementation Details

### Core Components

#### 1. EmployeeIngestBatch Entity
```java
@Entity
@Table(name = "employee_ingest_batch")
public class EmployeeIngestBatch {
    @Id
    private String id;
    
    @Column(name = "ingest_date", nullable = false)
    private LocalDateTime ingestDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;
    
    private Integer totalRecords = 0;
    private Integer newRecords = 0;
    private Integer updatedRecords = 0;
    private Integer deletedRecords = 0;
    
    // getters, setters, constructors...
}
```

#### 2. EmployeeSnapshot Entity
```java
@Entity
@Table(name = "employee_snapshot")
public class EmployeeSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "batch_id", nullable = false)
    private String batchId;
    
    @Column(name = "employee_id")
    private Long employeeId;
    
    private String name;
    private Integer age;
    private String status;
    private LocalDate dob;
    
    @Column(name = "snapshot_date", nullable = false)
    private LocalDateTime snapshotDate;
    
    // Factory method for creating from Employee
    public static EmployeeSnapshot fromEmployee(Employee employee, String batchId) {
        return new EmployeeSnapshot(batchId, employee.getId(), 
            employee.getName(), employee.getAge(), 
            employee.getStatus(), employee.getDob());
    }
}
```

#### 3. EmployeeDelta Entity
```java
@Entity
@Table(name = "employee_delta")
public class EmployeeDelta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "batch_id", nullable = false)
    private String batchId;
    
    @Column(name = "employee_id")
    private Long employeeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "delta_type", nullable = false)
    private DeltaType deltaType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_values", columnDefinition = "jsonb")
    private Map<String, Object> previousValues;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_values", columnDefinition = "jsonb")
    private Map<String, Object> currentValues;
    
    @Column(name = "changed_fields")
    private String[] changedFields;
    
    private String summary;
    
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}
```

### Service Layer

#### EmployeeDeltaServiceImpl
The core service responsible for delta detection:

```java
@Service
@Transactional
public class EmployeeDeltaServiceImpl implements EmployeeDeltaService {
    
    @Override
    public void detectAndStoreDeltas(String batchId, List<Employee> currentEmployees) {
        // 1. Create ingest batch record
        EmployeeIngestBatch batch = createIngestBatch(batchId);
        
        // 2. Find previous batch for comparison
        Optional<EmployeeIngestBatch> previousBatch = findPreviousCompletedBatch();
        
        if (previousBatch.isEmpty()) {
            // First batch - mark all as NEW
            handleFirstBatch(batchId, currentEmployees);
        } else {
            // Compare with previous batch
            performDeltaComparison(batchId, currentEmployees, previousBatch.get().getId());
        }
        
        // 3. Create snapshots for current employees
        createSnapshots(batchId, currentEmployees);
        
        // 4. Update batch status
        updateBatchStatus(batchId, BatchStatus.COMPLETED);
    }
    
    private void performDeltaComparison(String currentBatchId, 
                                      List<Employee> currentEmployees, 
                                      String previousBatchId) {
        // Get previous snapshots
        List<EmployeeSnapshot> previousSnapshots = 
            employeeSnapshotRepository.findByBatchId(previousBatchId);
        
        Map<Long, EmployeeSnapshot> previousMap = previousSnapshots.stream()
            .collect(Collectors.toMap(EmployeeSnapshot::getEmployeeId, e -> e));
        
        Map<Long, Employee> currentMap = currentEmployees.stream()
            .collect(Collectors.toMap(Employee::getId, e -> e));
        
        // Detect NEW and UPDATED employees
        for (Employee current : currentEmployees) {
            EmployeeSnapshot previous = previousMap.get(current.getId());
            
            if (previous == null) {
                // NEW employee
                createDelta(currentBatchId, current.getId(), DeltaType.NEW, 
                           null, createCurrentValues(current), null, 
                           "New employee added");
            } else if (hasChanges(previous, current)) {
                // UPDATED employee
                List<String> changedFields = getChangedFields(previous, current);
                createDelta(currentBatchId, current.getId(), DeltaType.UPDATED,
                           createPreviousValues(previous), createCurrentValues(current),
                           changedFields.toArray(new String[0]),
                           "Employee updated: " + String.join(", ", changedFields));
            }
        }
        
        // Detect DELETED employees
        for (EmployeeSnapshot previous : previousSnapshots) {
            if (!currentMap.containsKey(previous.getEmployeeId())) {
                createDelta(currentBatchId, previous.getEmployeeId(), DeltaType.DELETED,
                           createPreviousValues(previous), null, null,
                           "Employee deleted");
            }
        }
    }
    
    private boolean hasChanges(EmployeeSnapshot previous, Employee current) {
        return !Objects.equals(previous.getName(), current.getName()) ||
               !Objects.equals(previous.getAge(), current.getAge()) ||
               !Objects.equals(previous.getStatus(), current.getStatus()) ||
               !Objects.equals(previous.getDob(), current.getDob());
    }
    
    private List<String> getChangedFields(EmployeeSnapshot previous, Employee current) {
        List<String> changedFields = new ArrayList<>();
        
        if (!Objects.equals(previous.getName(), current.getName())) {
            changedFields.add("name");
        }
        if (!Objects.equals(previous.getAge(), current.getAge())) {
            changedFields.add("age");
        }
        if (!Objects.equals(previous.getStatus(), current.getStatus())) {
            changedFields.add("status");
        }
        if (!Objects.equals(previous.getDob(), current.getDob())) {
            changedFields.add("dob");
        }
        
        return changedFields;
    }
}
```

### Enhanced CSV Ingest Process

The existing EmployeeIngestServiceImpl was enhanced to integrate delta detection:

```java
@Service
public class EmployeeIngestServiceImpl extends AbstractEmployeeService implements EmployeeIngestService {
    
    private final EmployeeDeltaService deltaService;
    
    public EmployeeIngestServiceImpl(EmployeeRepository employeeRepository,
                                   EmployeeCsvIngestProperties props,
                                   EmployeeDeltaService deltaService) {
        super(employeeRepository, props);
        this.deltaService = deltaService;
    }
    
    protected synchronized IngestResult processCSVFileWithDelta(Path file, Path processedDir, String batchId) {
        try {
            // 1. Read and process CSV data
            List<Map<String, String>> csvData = CsvUtils.readCsvFile(file, ',');
            
            List<Employee> employees = csvData.stream().map(csvRow -> {
                Employee emp = new Employee();
                // Map CSV columns to Employee fields...
                return emp;
            }).collect(Collectors.toList());
            
            // 2. Save employees to database
            List<Employee> savedEmployees = employeeRepository.saveAll(employees);
            
            // 3. Perform delta detection
            performDeltaDetection(batchId, savedEmployees.size());
            
            // 4. Move processed file
            moveProcessedFile(file, processedDir);
            
            return new IngestResult(savedEmployees.size(), employees.size());
            
        } catch (Exception e) {
            log.error("Error processing CSV file: {}", file, e);
            throw new RuntimeException("Failed to process CSV file", e);
        }
    }
    
    private void performDeltaDetection(String batchId, int totalProcessed) {
        log.info("Starting delta detection for batch: {}", batchId);
        
        try {
            // Get current employees for this batch
            List<Employee> currentEmployees = employeeRepository.findByBatchId(batchId);
            
            // Perform delta detection
            deltaService.detectAndStoreDeltas(batchId, currentEmployees);
            
            // Get delta counts for logging
            Map<DeltaType, Long> deltaCounts = deltaService.getDeltaCountsByBatch(batchId);
            
            log.info("Delta detection completed for batch: {} - NEW: {}, UPDATED: {}, DELETED: {}",
                    batchId,
                    deltaCounts.getOrDefault(DeltaType.NEW, 0L),
                    deltaCounts.getOrDefault(DeltaType.UPDATED, 0L),
                    deltaCounts.getOrDefault(DeltaType.DELETED, 0L));
                    
        } catch (Exception e) {
            log.error("Error during delta detection for batch: {}", batchId, e);
            throw new RuntimeException("Delta detection failed", e);
        }
    }
}
```

## REST API Endpoints

### EmployeeDeltaController

```java
@RestController
@RequestMapping("/api/deltas")
public class EmployeeDeltaController {
    
    private final EmployeeDeltaService deltaService;
    
    @GetMapping("/batches")
    public ResponseEntity<List<EmployeeIngestBatch>> getAllBatches() {
        return ResponseEntity.ok(deltaService.getAllBatches());
    }
    
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<EmployeeIngestBatch> getBatch(@PathVariable String batchId) {
        return deltaService.getBatchById(batchId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/batches/{batchId}/deltas")
    public ResponseEntity<List<EmployeeDelta>> getDeltasByBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(deltaService.getDeltasByBatch(batchId));
    }
    
    @GetMapping("/by-type/{deltaType}")
    public ResponseEntity<List<EmployeeDelta>> getDeltasByType(@PathVariable DeltaType deltaType) {
        return ResponseEntity.ok(deltaService.getDeltasByType(deltaType));
    }
    
    @GetMapping("/by-date-range")
    public ResponseEntity<List<EmployeeDelta>> getDeltasByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(deltaService.getDeltasByDateRange(from, to));
    }
}
```

### API Usage Examples

#### Get All Batches
```bash
curl -s http://localhost:8081/api/deltas/batches | jq .
```

#### Get Specific Batch Details
```bash
curl -s http://localhost:8081/api/deltas/batches/{batchId} | jq .
```

#### Get All NEW Employees
```bash
curl -s http://localhost:8081/api/deltas/by-type/NEW | jq .
```

#### Get Deltas by Date Range
```bash
curl -s "http://localhost:8081/api/deltas/by-date-range?from=2025-09-01T00:00:00&to=2025-09-30T23:59:59" | jq .
```

## Automated Reporting

### EmployeeDeltaReportJob

Quartz-scheduled job for generating CSV reports:

```java
@Component
public class EmployeeDeltaReportJob implements Job {
    
    private static final Logger log = LoggerFactory.getLogger(EmployeeDeltaReportJob.class);
    
    @Autowired
    private EmployeeDeltaService deltaService;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Starting Employee Delta Report Job");
        
        try {
            // Generate summary report
            generateSummaryReport();
            
            // Generate detailed reports by delta type
            generateDetailedDeltaReports();
            
            log.info("Employee Delta Report Job completed successfully");
            
        } catch (Exception e) {
            log.error("Error in Employee Delta Report Job", e);
            throw new JobExecutionException("Failed to generate delta reports", e);
        }
    }
    
    private void generateSummaryReport() throws IOException {
        List<EmployeeIngestBatch> recentBatches = 
            deltaService.getRecentBatches(30); // Last 30 days
        
        List<Map<String, String>> summaryData = recentBatches.stream()
            .map(this::batchToSummaryRow)
            .collect(Collectors.toList());
        
        Path summaryReportPath = Paths.get("./.data/reports/delta-summary-report.csv");
        CsvUtils.writeCsvFile(summaryData, summaryReportPath, ',');
        
        log.info("Generated summary report: {}", summaryReportPath);
    }
    
    private void generateDetailedDeltaReports() throws IOException {
        for (DeltaType deltaType : DeltaType.values()) {
            List<EmployeeDelta> deltas = deltaService.getRecentDeltasByType(deltaType, 30);
            
            if (!deltas.isEmpty()) {
                List<Map<String, String>> deltaData = deltas.stream()
                    .map(this::deltaToCsvRow)
                    .collect(Collectors.toList());
                
                Path reportPath = Paths.get("./.data/reports/delta-" + 
                    deltaType.name().toLowerCase() + "-report.csv");
                CsvUtils.writeCsvFile(deltaData, reportPath, ',');
                
                log.info("Generated {} delta report: {} ({} records)", 
                        deltaType, reportPath, deltas.size());
            }
        }
    }
}
```

## CSV Column Mapping Configuration

The system expects specific column names in CSV files as configured in `application.yml`:

```yaml
ingest:
  csv:
    employees:
      columnMapping:
        person_id: id
        full_name: name
        years: age
        date_of_birth: dob
        # status column maps directly
```

### Required CSV Format
```csv
person_id,full_name,years,status,date_of_birth
1,John Doe,30,ACTIVE,1994-01-15
2,Jane Smith,25,ACTIVE,1999-03-22
3,Bob Johnson,35,INACTIVE,1989-07-08
```

## Testing

### Test Data Files

Created comprehensive test files in `/test-data/`:

#### emp-initial.csv (First batch)
```csv
person_id,full_name,years,status,date_of_birth
1,John Doe,30,ACTIVE,1994-01-15
2,Jane Smith,25,ACTIVE,1999-03-22
3,Bob Johnson,35,INACTIVE,1989-07-08
4,Alice Brown,28,ACTIVE,1996-11-30
```

#### emp-updated.csv (Second batch with changes)
```csv
person_id,full_name,years,status,date_of_birth
1,John Doe,31,ACTIVE,1994-01-15          # UPDATED: age changed
2,Jane Smith,25,INACTIVE,1999-03-22      # UPDATED: status changed
4,Alice Brown,28,ACTIVE,1996-11-30       # No change
5,Charlie Wilson,40,ACTIVE,1984-05-12    # NEW: employee added
6,Diana Miller,33,ACTIVE,1991-09-18      # NEW: employee added
# Bob Johnson (ID 3) missing - DELETED
```

Expected Delta Detection Results:
- **NEW**: 2 records (Charlie Wilson, Diana Miller)
- **UPDATED**: 2 records (John Doe age change, Jane Smith status change)
- **DELETED**: 1 record (Bob Johnson)

## System Logs

Successful processing logs show:
```
Creating ingest batch: 1f586c59-76a3-4aa1-b934-be12c27c9195
Detecting deltas for batch: 1f586c59-76a3-4aa1-b934-be12c27c9195  
Comparing with previous batch: b72d3e76-435a-44f4-a6bf-714f158da601
Delta detection completed - NEW: 0, UPDATED: 0, DELETED: 0
Updating ingest batch with status: COMPLETED
```

## Performance Considerations

### Database Optimization
- **Indexes**: Created on frequently queried columns (batch_id, employee_id, delta_type, created_date)
- **Partitioning**: Consider partitioning delta tables by date for large datasets
- **Archival**: Implement data retention policies for old snapshots and deltas

### Memory Optimization
- **Streaming**: For large CSV files, consider streaming processing instead of loading all data into memory
- **Batch Processing**: Process deltas in batches for very large datasets

## Monitoring and Alerting

### Key Metrics to Monitor
- **Batch Processing Time**: Time taken for delta detection
- **Delta Counts**: Number of NEW/UPDATED/DELETED records per batch
- **Failed Batches**: Monitor for failed ingestion processes
- **Database Growth**: Track table sizes for maintenance planning

### Recommended Alerts
- Failed batch processing
- Unusual spike in deleted records
- Processing time exceeding thresholds
- Database connection issues

## Future Enhancements

### Potential Improvements
1. **Field-Level Change Tracking**: More granular tracking of which specific fields changed
2. **Delta Approval Workflow**: Manual approval process for significant changes
3. **Rollback Capability**: Ability to rollback to previous employee states
4. **Advanced Reporting**: Statistical analysis of change patterns
5. **Real-time Notifications**: Immediate alerts for critical changes
6. **Data Quality Checks**: Validation rules for incoming data
7. **Audit Trail**: Complete audit log of all system changes

## Deployment Notes

### Prerequisites
- Java 21
- PostgreSQL 16.3+
- Spring Boot 3.5.6
- Maven 3.8+

### Configuration
1. Update database connection in `application.yml`
2. Configure CSV file paths and column mappings
3. Set up Quartz scheduler properties
4. Configure logging levels as needed

### Migration
Run Flyway migration V8 to create delta tracking tables:
```bash
mvn flyway:migrate
```

### Testing
```bash
# Build project
mvn clean install

# Start database
docker-compose -f .docker/docker-compose.yml up db -d

# Run application
mvn spring-boot:run -pl web
```

## Troubleshooting

### Common Issues

#### Column Mapping Errors
- **Problem**: CSV processing results in 0 records
- **Solution**: Verify CSV column names match `application.yml` configuration

#### Database Connection Issues
- **Problem**: Application fails to start with connection refused
- **Solution**: Ensure PostgreSQL is running and accessible

#### Port Conflicts
- **Problem**: Web server fails to start (port in use)
- **Solution**: Kill existing processes or change server port

#### Memory Issues
- **Problem**: OutOfMemoryError during large file processing
- **Solution**: Increase JVM heap size or implement streaming processing

---

*Generated on September 27, 2025 for platform-scheduler delta detection system*