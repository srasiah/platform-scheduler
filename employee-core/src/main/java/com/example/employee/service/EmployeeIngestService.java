package com.example.employee.service;

import java.nio.file.Path;

public interface EmployeeIngestService {
    void ingestFromDirectory(Path ingestDir, Path processedDir);
    void ingestFromDirectory();
}
