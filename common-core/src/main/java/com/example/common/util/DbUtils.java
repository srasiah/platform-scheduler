package com.example.common.util;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public class DbUtils {
    /**
     * Generic method to map CSV records to entities and save them using a JPA repository.
     * @param records List of String[] from CSV (header row should be handled by caller)
     * @param mapper  Function to map String[] to an entity instance
     * @param repo    JPA repository for the entity
     * @param <T>     Entity type
     */
    public static <T> void importCsvRecords(List<String[]> records, java.util.function.Function<String[], T> mapper, JpaRepository<T, ?> repo) {
        List<T> entities = records.stream()
                .map(mapper)
                .toList();
        repo.saveAll(entities);
    }

    /**
     * Generic method to read all entities from a JPA repository and map them to CSV records (String[]).
     * @param repo   JPA repository for the entity
     * @param mapper Function to map entity to String[]
     * @param <T>   Entity type
     * @return List of String[] representing CSV records
     */
    public static <T> List<String[]> readCsvRecords(JpaRepository<T, ?> repo, java.util.function.Function<T, String[]> mapper) {
        return repo.findAll().stream()
                .map(mapper)
                .toList();
    }
    /**
     * Generic method to update entities in the database from CSV records.
     * The mapper should return an entity with the correct ID set for updating.
     * @param records List of String[] from CSV (header row should be handled by caller)
     * @param mapper  Function to map String[] to an entity instance (with ID set)
     * @param repo    JPA repository for the entity
     * @param <T>     Entity type
     */
    public static <T> void updateCsvRecords(List<String[]> records, java.util.function.Function<String[], T> mapper, JpaRepository<T, ?> repo) {
        List<T> entities = records.stream()
                .map(mapper)
                .toList();
        repo.saveAll(entities); // saveAll will update if ID exists, insert if not
    }
}
