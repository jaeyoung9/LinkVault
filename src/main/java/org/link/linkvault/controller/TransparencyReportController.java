package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.service.TransparencyReportService;
import org.link.linkvault.service.SystemSettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class TransparencyReportController {

    private final TransparencyReportService reportService;

    @GetMapping("/transparency")
    public String list(Model model) {
        model.addAttribute("reports", reportService.findPublished());
        model.addAttribute("pageTitle", "Transparency Reports");
        return "transparency";
    }

    @GetMapping("/transparency/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("report", reportService.findById(id));
        model.addAttribute("pageTitle", "Transparency Report");
        return "transparency-detail";
    }
}
