package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.service.FileVaultService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileVaultService fileVaultService;

    @GetMapping("/photos/{filename}")
    public ResponseEntity<Resource> servePhoto(@PathVariable String filename) throws IOException {
        Resource resource = fileVaultService.loadAsResource(filename);
        String contentType = Files.probeContentType(Path.of(resource.getFile().getAbsolutePath()));
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
