package com.example.employee.service;

import com.example.employee.entity.Employee;
import com.example.employee.repo.EmployeeRepository;
import com.example.employee.config.EmployeeCsvIngestProperties;
import com.example.common.util.CsvUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
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

    @BeforeEach
    void setUp() throws Exception {
        employeeRepository = mock(EmployeeRepository.class);
        props = mock(EmployeeCsvIngestProperties.class);
        service = new EmployeeIngestServiceImpl();
        // Inject mocks using reflection
        Field repoField = EmployeeIngestServiceImpl.class.getDeclaredField("employeeRepository");
        repoField.setAccessible(true);
        repoField.set(service, employeeRepository);
        Field propsField = EmployeeIngestServiceImpl.class.getDeclaredField("props");
        propsField.setAccessible(true);
        propsField.set(service, props);
    }

    @Test
    void testIngestFromDirectory_ReadsCsvAndSavesEmployees() throws Exception {
        // Arrange
        Path ingestDir = Path.of("/tmp/ingest");
        Path processedDir = Path.of("/tmp/processed");
        String[] header = {"id", "name", "age", "status"};
        String[] row1 = {"1", "Alice", "30", ""};
        String[] row2 = {"2", "Bob", "40", ""};
        List<String[]> records = List.of(header, row1, row2);
        when(props.getFileNamePrefix()).thenReturn("emp-");
        when(props.getColumnMapping()).thenReturn(Map.of("id","id","name","name","age","age","status","status"));
        when(props.getDefaultStatus()).thenReturn("IN_PROGRESS");
        // Mock CsvUtils.readCsv
        try (MockedStatic<CsvUtils> csvUtilsMock = Mockito.mockStatic(CsvUtils.class)) {
            csvUtilsMock.when(() -> CsvUtils.readCsv(any())).thenReturn(records);
            // Mock Files.list
            try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
                filesMock.when(() -> Files.list(ingestDir)).thenReturn(java.util.stream.Stream.of(ingestDir.resolve("emp-1.csv")));
                filesMock.when(() -> Files.exists(any())).thenReturn(true);
                filesMock.when(() -> Files.move(any(), any())).thenReturn(processedDir.resolve("emp-1-123.csv"));
                // Act
                service.ingestFromDirectory(ingestDir, processedDir);
                // Assert
                verify(employeeRepository).saveAll(any());
            }
        }
    }

    @Test
    void testIngestFromDirectory_EmptyCsv() throws Exception {
        Path ingestDir = Path.of("/tmp/ingest");
        Path processedDir = Path.of("/tmp/processed");
        when(props.getFileNamePrefix()).thenReturn("emp-");
        when(props.getColumnMapping()).thenReturn(Map.of("id","id"));
        when(props.getDefaultStatus()).thenReturn("IN_PROGRESS");
        try (MockedStatic<CsvUtils> csvUtilsMock = Mockito.mockStatic(CsvUtils.class)) {
            csvUtilsMock.when(() -> CsvUtils.readCsv(any())).thenReturn(List.of());
            try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
                filesMock.when(() -> Files.list(ingestDir)).thenReturn(java.util.stream.Stream.of(ingestDir.resolve("emp-1.csv")));
                filesMock.when(() -> Files.exists(any())).thenReturn(true);
                filesMock.when(() -> Files.move(any(), any())).thenReturn(processedDir.resolve("emp-1-123.csv"));
                service.ingestFromDirectory(ingestDir, processedDir);
                verify(employeeRepository, never()).saveAll(any());
            }
        }
    }
}
