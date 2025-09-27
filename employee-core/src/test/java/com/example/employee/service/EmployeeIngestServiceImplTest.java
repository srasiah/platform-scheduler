package com.example.employee.service;

import com.example.employee.repo.EmployeeRepository;
import com.example.employee.service.impl.EmployeeIngestServiceImpl;
import com.example.employee.config.EmployeeCsvIngestProperties;
import com.example.common.util.CsvUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeIngestServiceImplTest {
    private EmployeeIngestServiceImpl service;
    private EmployeeRepository employeeRepository;
    private EmployeeCsvIngestProperties props;
    private EmployeeDeltaService deltaService;

    @BeforeEach
    void setUp() throws Exception {
        employeeRepository = mock(EmployeeRepository.class);
        props = mock(EmployeeCsvIngestProperties.class);
        deltaService = mock(EmployeeDeltaService.class);
        service = new EmployeeIngestServiceImpl(employeeRepository, props, deltaService);
    }

    @Test
    void testIngestFromDirectory_ReadsCsvAndSavesEmployees() throws Exception {
        // Arrange
        Path ingestDir = Path.of("/tmp/ingest");
        Path processedDir = Path.of("/tmp/processed");
        Map<String, String> row1 = Map.of("person_id", "1", "name", "Alice", "age", "30", "status", "");
        Map<String, String> row2 = Map.of("person_id", "2", "name", "Bob", "age", "40", "status", "");
        List<Map<String, String>> csvData = List.of(row1, row2);
        
        when(props.getFileNamePrefix()).thenReturn("emp-");
        when(props.getColumnMapping()).thenReturn(Map.of("person_id","id","name","name","age","age","status","status"));
        when(props.getDefaultStatus()).thenReturn("IN_PROGRESS");
        when(employeeRepository.findAllById(any())).thenReturn(List.of()); // No existing employees
        
        // Mock CsvUtils.readCsvFile (note: different method signature)
        try (MockedStatic<CsvUtils> csvUtilsMock = Mockito.mockStatic(CsvUtils.class)) {
            csvUtilsMock.when(() -> CsvUtils.readCsvFile(any(Path.class), any(Character.class))).thenReturn(csvData);
            // Mock Files.list
            try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
                filesMock.when(() -> Files.list(ingestDir)).thenReturn(java.util.stream.Stream.of(ingestDir.resolve("emp-1.csv")));
                filesMock.when(() -> Files.exists(any())).thenReturn(true);
                filesMock.when(() -> Files.createDirectories(any())).thenReturn(processedDir);
                filesMock.when(() -> Files.move(any(), any())).thenReturn(processedDir.resolve("emp-1-123.csv"));
                // Act
                service.ingestFromDirectory(ingestDir, processedDir);
                // Assert
                verify(employeeRepository).saveAll(any());
                verify(employeeRepository).findAllById(any());
            }
        }
    }

    @Test
    void testIngestFromDirectory_EmptyCsv() throws Exception {
        Path ingestDir = Path.of("/tmp/ingest");
        Path processedDir = Path.of("/tmp/processed");
        when(props.getFileNamePrefix()).thenReturn("emp-");
        when(props.getColumnMapping()).thenReturn(Map.of("person_id","id"));
        when(props.getDefaultStatus()).thenReturn("IN_PROGRESS");
        
        try (MockedStatic<CsvUtils> csvUtilsMock = Mockito.mockStatic(CsvUtils.class)) {
            csvUtilsMock.when(() -> CsvUtils.readCsvFile(any(Path.class), any(Character.class))).thenReturn(List.of());
            try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
                filesMock.when(() -> Files.list(ingestDir)).thenReturn(java.util.stream.Stream.of(ingestDir.resolve("emp-1.csv")));
                filesMock.when(() -> Files.exists(any())).thenReturn(true);
                filesMock.when(() -> Files.createDirectories(any())).thenReturn(processedDir);
                filesMock.when(() -> Files.move(any(), any())).thenReturn(processedDir.resolve("emp-1-123.csv"));
                service.ingestFromDirectory(ingestDir, processedDir);
                verify(employeeRepository, never()).saveAll(any());
            }
        }
    }
}
