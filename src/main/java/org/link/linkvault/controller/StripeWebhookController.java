package org.link.linkvault.controller;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeService stripeService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                 @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = stripeService.verifyWebhookEvent(payload, sigHeader);
        } catch (SecurityException e) {
            log.warn("Invalid Stripe webhook signature");
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.error("Stripe webhook verification error", e);
            return ResponseEntity.badRequest().body("Webhook error");
        }

        String eventType = event.getType();
        log.info("Stripe webhook received: {}", eventType);

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);

        switch (eventType) {
            case "checkout.session.completed":
                if (stripeObject instanceof Session) {
                    stripeService.handleCheckoutCompleted((Session) stripeObject);
                }
                break;
            case "charge.refunded":
                log.info("Charge refunded event received");
                break;
            case "invoice.paid":
                log.info("Recurring invoice paid");
                break;
            case "customer.subscription.deleted":
                log.info("Subscription cancelled");
                break;
            case "invoice.payment_failed":
                log.warn("Invoice payment failed");
                break;
            default:
                log.debug("Unhandled Stripe event: {}", eventType);
        }

        return ResponseEntity.ok("OK");
    }
}
