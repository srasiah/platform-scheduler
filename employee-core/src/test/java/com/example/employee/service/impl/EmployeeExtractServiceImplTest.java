package com.example.employee.service.impl;

import com.example.employee.entity.Employee;
import com.example.employee.repo.EmployeeRepository;
import com.example.employee.service.impl.EmployeeExtractServiceImpl;
import com.example.employee.config.EmployeeCsvExtractProperties;
import com.example.common.util.CsvUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeExtractServiceImplTest {
    private EmployeeExtractServiceImpl service;
    private EmployeeRepository employeeRepository;
    private EmployeeCsvExtractProperties props;

    @BeforeEach
    void setUp() throws Exception {
        employeeRepository = mock(EmployeeRepository.class);
        props = mock(EmployeeCsvExtractProperties.class);
        service = new EmployeeExtractServiceImpl(employeeRepository, props);
    }

    @Test
    void testExtractToDirectory_WritesCsvAndUpdatesStatus() {
        // Arrange
        Employee emp1 = new Employee();
        emp1.setId(1L); emp1.setName("Alice"); emp1.setAge(30); emp1.setStatus("READY");
        Employee emp2 = new Employee();
        emp2.setId(2L); emp2.setName("Bob"); emp2.setAge(40); emp2.setStatus("READY");
        List<Employee> employees = Arrays.asList(emp1, emp2);
        when(employeeRepository.findByStatus("READY")).thenReturn(employees);
        when(props.getColumnMapping()).thenReturn(Map.of("id","id","name","name","age","age","status","status"));
        when(props.getFileNamePrefix()).thenReturn("emp-");
        when(props.getExtractedStatus()).thenReturn("EXTRACTED");
        // Mock CsvUtils.writeCsv
        try (MockedStatic<CsvUtils> csvUtilsMock = Mockito.mockStatic(CsvUtils.class)) {
            // Act
            service.extractToDirectory(Path.of("/tmp"), "READY");
            // Assert
            csvUtilsMock.verify(() -> CsvUtils.writeCsv(any(), any()), times(1));
            verify(employeeRepository).saveAll(any());
            // Check that status is updated
            assert emp1.getStatus().equals("EXTRACTED");
            assert emp2.getStatus().equals("EXTRACTED");
        }
    }

    @Test
    void testExtractToDirectory_NoEmployeesWithStatus() {
        when(employeeRepository.findByStatus("READY")).thenReturn(List.of());
        when(props.getColumnMapping()).thenReturn(Map.of("id","id"));
        service.extractToDirectory(Path.of("/tmp"), "READY");
        verify(employeeRepository, never()).saveAll(any());
    }
}
