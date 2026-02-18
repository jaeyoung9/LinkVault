package org.link.linkvault.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/payment")
public class PaymentWebController {

    @GetMapping("/success")
    public String paymentSuccess(Model model) {
        model.addAttribute("pageTitle", "Payment Successful");
        return "payment-success";
    }

    @GetMapping("/cancel")
    public String paymentCancel(Model model) {
        model.addAttribute("pageTitle", "Payment Cancelled");
        return "payment-cancel";
    }
}
