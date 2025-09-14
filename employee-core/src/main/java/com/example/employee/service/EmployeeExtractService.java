package com.example.employee.service;

import java.nio.file.Path;

public interface EmployeeExtractService {
    void extractToDirectory(Path extractDir, String extractedStatues);
    void extractToDirectory();
}
