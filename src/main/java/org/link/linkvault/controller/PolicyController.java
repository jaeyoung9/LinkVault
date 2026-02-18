package org.link.linkvault.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/policies")
public class PolicyController {

    @GetMapping("/refund-policy")
    public String refundPolicy() {
        return "policies/refund-policy";
    }

    @GetMapping("/ad-policy")
    public String adPolicy() {
        return "policies/ad-policy";
    }

    @GetMapping("/payment-policy")
    public String paymentPolicy() {
        return "policies/payment-policy";
    }
}
