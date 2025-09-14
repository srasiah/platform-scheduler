package com.example.common.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.opencsv.CSVReader;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;

public class CsvUtils {
    private static final Logger log = LoggerFactory.getLogger(CsvUtils.class);
    /**
     * Reads a CSV file from the given path and returns a list of records (each record is a String array).
     * Handles quoted fields and custom separators using OpenCSV.
     * @param filePath Path to the CSV file
     * @return List of String[] records
     */
    public static List<String[]> readCsv(String filePath) throws IOException {
    log.info("Reading CSV file: {}", filePath);
    log.debug("readCsv called with filePath={}", filePath);
        return readCsv(filePath, ',');
    }

    /**
     * Reads a CSV file with a custom separator.
     * @param filePath Path to the CSV file
     * @param separator The separator character (e.g. ',' or ';')
     * @return List of String[] records
     */
    public static List<String[]> readCsv(String filePath, char separator) throws IOException {
    log.info("Reading CSV file: {} with separator '{}'", filePath, separator);
    log.debug("readCsv called with filePath={}, separator={}", filePath, separator);
        try (Reader reader = Files.newBufferedReader(Path.of(filePath));
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                     .build()) {
            List<String[]> records = new ArrayList<>();
            String[] nextLine;
            try {
                int row = 0;
                while ((nextLine = csvReader.readNext()) != null) {
                    records.add(nextLine);
                    log.debug("Read row {}: {}", row++, java.util.Arrays.toString(nextLine));
                }
                log.info("Read {} records from {}", records.size(), filePath);
            } catch (Exception e) {
                log.error("Error reading CSV file: {}", filePath, e);
                throw new IOException("Error reading CSV file: " + filePath, e);
            }
            return records;
        }
    }

    /**
     * Returns a list of file paths for all CSV files in the given folder (non-recursive).
     */
    public static List<String> listCsvFiles(String folderPath) throws IOException {
    log.info("Listing CSV files in folder: {}", folderPath);
    log.debug("listCsvFiles called with folderPath={}", folderPath);
        try (var stream = Files.list(Path.of(folderPath))) {
            List<String> files = stream
                    .filter(p -> !Files.isDirectory(p))
                    .map(Path::toString)
                    .filter(f -> f.toLowerCase().endsWith(".csv"))
                    .toList();
            log.info("Found {} CSV files in folder {}", files.size(), folderPath);
            for (String f : files) {
                log.debug("Found CSV file: {}", f);
            }
            return files;
        }
    }

    /**
     * Writes a list of records to a CSV file at the given path using OpenCSV.
     * Handles quoted fields, commas, and special characters.
     * @param filePath Path to the CSV file
     * @param rows List of String[] records (first row should be header)
     * @throws IOException if writing fails
     */
    public static void writeCsv(String filePath, List<String[]> rows) throws IOException {
        log.info("Writing CSV file: {} ({} rows)", filePath, rows != null ? rows.size() : 0);
        try (CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Path.of(filePath)))) {
            if (rows != null) {
                for (String[] row : rows) {
                    writer.writeNext(row);
                }
            }
        } catch (Exception e) {
            log.error("Error writing CSV file: {}", filePath, e);
            throw new IOException("Error writing CSV file: " + filePath, e);
        }
    }
}
