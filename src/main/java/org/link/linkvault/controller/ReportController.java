package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.ReportRequestDto;
import org.link.linkvault.dto.ReportResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.ReportService;
import org.link.linkvault.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('REPORT_SUBMIT')")
    public ResponseEntity<ReportResponseDto> submitReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReportRequestDto dto) {
        User reporter = userService.getUserEntity(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.submit(reporter, dto));
    }
}
