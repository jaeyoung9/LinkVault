package org.link.linkvault.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileVaultService {

    @Value("${linkvault.file-vault.upload-path:./uploads/photos}")
    private String uploadPath;

    @Value("${linkvault.file-vault.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String allowedTypesStr;

    @Value("${linkvault.file-vault.max-file-size-mb:10}")
    private int maxFileSizeMb;

    private Path uploadDir;
    private List<String> allowedTypes;

    private SystemSettingsService systemSettingsService;

    @Autowired
    public void setSystemSettingsService(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    @PostConstruct
    public void init() throws IOException {
        // Use @Value defaults first; DB overrides applied after DataInitializer seeds
        uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        allowedTypes = Arrays.asList(allowedTypesStr.split(","));
        log.info("FileVault initialized: path={}, allowedTypes={}, maxSize={}MB", uploadDir, allowedTypes, maxFileSizeMb);
    }

    public void reloadSettings() {
        try {
            systemSettingsService.getValue("file-vault.upload-path").ifPresent(val -> {
                uploadPath = val;
                try {
                    uploadDir = Paths.get(val).toAbsolutePath().normalize();
                    Files.createDirectories(uploadDir);
                } catch (IOException e) {
                    log.warn("Failed to create upload directory from DB setting: {}", val, e);
                }
            });
            systemSettingsService.getValue("file-vault.allowed-types").ifPresent(val -> {
                allowedTypesStr = val;
                allowedTypes = Arrays.asList(val.split(","));
            });
            systemSettingsService.getValue("file-vault.max-file-size-mb").ifPresent(val -> {
                try {
                    maxFileSizeMb = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    log.warn("Invalid max-file-size-mb value from DB: {}", val);
                }
            });
            log.info("FileVault settings reloaded: path={}, allowedTypes={}, maxSize={}MB", uploadDir, allowedTypes, maxFileSizeMb);
        } catch (Exception e) {
            log.warn("Failed to reload FileVault settings from DB, using current values", e);
        }
    }

    public String store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }

        long maxBytes = (long) maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File size exceeds maximum of " + maxFileSizeMb + "MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        Path targetPath = uploadDir.resolve(filename).normalize();
        if (!targetPath.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file: {} -> {}", originalFilename, filename);
        return filename;
    }

    public Resource loadAsResource(String filename) {
        try {
            Path filePath = uploadDir.resolve(filename).normalize();
            if (!filePath.startsWith(uploadDir)) {
                throw new IllegalArgumentException("Invalid file path");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("File not found: " + filename);
        } catch (MalformedURLException e) {
            throw new RuntimeException("File not found: " + filename, e);
        }
    }

    public void delete(String filename) {
        try {
            Path filePath = uploadDir.resolve(filename).normalize();
            if (!filePath.startsWith(uploadDir)) {
                throw new IllegalArgumentException("Invalid file path");
            }
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", filename);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filename, e);
        }
    }
}
