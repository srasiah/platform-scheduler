package com.example.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "extract.csv.employees")
public class EmployeeCsvExtractProperties {
    private boolean enabled;
    private String fileFolder;
    private String fileNamePrefix;
    private String tableName;
    private Map<String, String> columnMapping;

    // getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getFileFolder() { return fileFolder; }
    public void setFileFolder(String fileFolder) { this.fileFolder = fileFolder; }
    public String getFileNamePrefix() { return fileNamePrefix; }
    public void setFileNamePrefix(String fileNamePrefix) { this.fileNamePrefix = fileNamePrefix; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public Map<String, String> getColumnMapping() { return columnMapping; }
    public void setColumnMapping(Map<String, String> columnMapping) { this.columnMapping = columnMapping; }
}
