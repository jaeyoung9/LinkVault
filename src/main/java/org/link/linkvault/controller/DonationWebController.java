package org.link.linkvault.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DonationWebController {

    @GetMapping("/donation/thankyou")
    public String thankYou(Model model) {
        model.addAttribute("pageTitle", "Thank You!");
        return "donation-thankyou";
    }
}
