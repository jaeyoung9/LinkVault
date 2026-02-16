package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseBackupService {

    private final JdbcTemplate jdbcTemplate;

    private static final String BACKUP_DIR = "backups";

    public String createBackup() {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "linkvault_backup_" + timestamp + ".sql";
        String fullPath = new File(dir, filename).getAbsolutePath().replace("\\", "/");

        jdbcTemplate.execute("SCRIPT TO '" + fullPath + "'");
        log.info("Database backup created: {}", filename);
        return filename;
    }

    public List<String> listBackups() {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>();
        }
        String[] files = dir.list((d, name) -> name.startsWith("linkvault_backup_") && name.endsWith(".sql"));
        if (files == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(files)
                .sorted((a, b) -> b.compareTo(a))
                .collect(Collectors.toList());
    }

    public void restoreBackup(String filename) {
        File file = new File(BACKUP_DIR, filename);
        if (!file.exists()) {
            throw new IllegalArgumentException("Backup file not found: " + filename);
        }

        String fullPath = file.getAbsolutePath().replace("\\", "/");
        jdbcTemplate.execute("RUNSCRIPT FROM '" + fullPath + "'");
        log.info("Database restored from backup: {}", filename);
    }
}
