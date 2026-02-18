package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseBackupService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    private static final String BACKUP_DIR = "backups";
    private static final Pattern BACKUP_FILENAME_PATTERN =
            Pattern.compile("^linkvault_backup_\\d{8}_\\d{6}\\.sql$");

    public String createBackup(String actorUsername) {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "linkvault_backup_" + timestamp + ".sql";
        String fullPath = new File(dir, filename).getAbsolutePath().replace("\\", "/");

        jdbcTemplate.execute("SCRIPT TO '" + fullPath + "'");
        log.info("Database backup created: {}", filename);
        auditLogService.log(actorUsername, AuditActionCodes.BACKUP_CREATE, "System", null,
                AuditDetailFormatter.format("filename", filename));
        return filename;
    }

    public List<String> listBackups() {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>();
        }
        String[] files = dir.list((d, name) -> BACKUP_FILENAME_PATTERN.matcher(name).matches());
        if (files == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(files)
                .sorted((a, b) -> b.compareTo(a))
                .collect(Collectors.toList());
    }

    public void restoreBackup(String filename, String actorUsername) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename must not be empty");
        }

        if (!BACKUP_FILENAME_PATTERN.matcher(filename).matches()) {
            throw new IllegalArgumentException("Invalid backup filename format");
        }

        File baseDir = new File(BACKUP_DIR);
        File file = new File(baseDir, filename);

        // Path traversal protection
        try {
            String basePath = baseDir.getCanonicalPath();
            String targetPath = file.getCanonicalPath();
            if (!targetPath.startsWith(basePath + File.separator) && !targetPath.equals(basePath)) {
                throw new IllegalArgumentException("Invalid backup filename");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid backup filename");
        }

        if (!file.exists()) {
            throw new IllegalArgumentException("Backup file not found: " + filename);
        }

        String fullPath = file.getAbsolutePath().replace("\\", "/");
        jdbcTemplate.execute("RUNSCRIPT FROM '" + fullPath + "'");
        log.info("Database restored from backup: {}", filename);
        auditLogService.log(actorUsername, AuditActionCodes.BACKUP_RESTORE, "System", null,
                AuditDetailFormatter.format("filename", filename));
    }
}
