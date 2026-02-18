package org.link.linkvault.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.entity.*;
import org.link.linkvault.repository.AdFreePassRepository;
import org.link.linkvault.repository.DonationRepository;
import org.link.linkvault.repository.StripeCustomerRepository;
import org.link.linkvault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final SystemSettingsService systemSettingsService;
    private final StripeCustomerRepository stripeCustomerRepository;
    private final AdFreePassRepository adFreePassRepository;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    private void initStripe() {
        String secretKey = systemSettingsService.getValue("stripe.api-key-secret").orElse("");
        if (secretKey.isEmpty()) {
            throw new IllegalStateException("Stripe API key not configured");
        }
        Stripe.apiKey = secretKey;
    }

    @Transactional
    public String createAdFreeCheckoutSession(User user, AdFreePassType passType, String successUrl, String cancelUrl) {
        initStripe();

        // Prevent duplicate purchase if active pass exists
        List<AdFreePass> activePasses = adFreePassRepository.findActiveByUserId(user.getId(), LocalDateTime.now());
        if (!activePasses.isEmpty()) {
            throw new IllegalStateException("You already have an active ad-free pass");
        }

        String priceId;
        if (passType == AdFreePassType.PURCHASE_7D) {
            priceId = systemSettingsService.getValue("stripe.adfree-7d-price-id").orElse("");
        } else {
            priceId = systemSettingsService.getValue("stripe.adfree-30d-price-id").orElse("");
        }

        if (priceId.isEmpty()) {
            throw new IllegalStateException("Stripe price not configured for " + passType);
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .putMetadata("userId", user.getId().toString())
                    .putMetadata("passType", passType.name())
                    .putMetadata("type", "ad_free_pass")
                    .setCustomerEmail(user.getEmail())
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Stripe checkout session creation failed", e);
            throw new RuntimeException("Payment service error: " + e.getMessage());
        }
    }

    @Transactional
    public String createDonationCheckoutSession(User user, int amountCents, DonationType donationType,
                                                 String successUrl, String cancelUrl) {
        initStripe();
        String currency = systemSettingsService.getValue("stripe.currency").orElse("usd");

        try {
            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .putMetadata("userId", user.getId().toString())
                    .putMetadata("type", "donation")
                    .putMetadata("donationType", donationType.name())
                    .setCustomerEmail(user.getEmail());

            if (donationType == DonationType.ONE_TIME) {
                builder.setMode(SessionCreateParams.Mode.PAYMENT)
                        .addLineItem(SessionCreateParams.LineItem.builder()
                                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency(currency)
                                        .setUnitAmount((long) amountCents)
                                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName("LinkVault Support - One-time")
                                                .build())
                                        .build())
                                .setQuantity(1L)
                                .build());
            } else {
                builder.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .addLineItem(SessionCreateParams.LineItem.builder()
                                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency(currency)
                                        .setUnitAmount((long) amountCents)
                                        .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                                .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                                .build())
                                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName("LinkVault Support - Monthly")
                                                .build())
                                        .build())
                                .setQuantity(1L)
                                .build());
            }

            Session session = Session.create(builder.build());
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Stripe donation checkout failed", e);
            throw new RuntimeException("Payment service error: " + e.getMessage());
        }
    }

    public Event verifyWebhookEvent(String payload, String sigHeader) {
        String webhookSecret = systemSettingsService.getValue("stripe.webhook-secret").orElse("");
        if (webhookSecret.isEmpty()) {
            throw new IllegalStateException("Stripe webhook secret not configured");
        }
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new SecurityException("Invalid Stripe webhook signature");
        }
    }

    @Transactional
    public void handleCheckoutCompleted(Session session) {
        Map<String, String> metadata = session.getMetadata();
        String type = metadata.getOrDefault("type", "");

        if ("ad_free_pass".equals(type)) {
            handleAdFreePassPayment(metadata, session.getPaymentIntent());
        } else if ("donation".equals(type)) {
            handleDonationPayment(metadata, session);
        }
    }

    private void handleAdFreePassPayment(Map<String, String> metadata, String paymentIntentId) {
        Long userId = Long.parseLong(metadata.get("userId"));
        AdFreePassType passType = AdFreePassType.valueOf(metadata.get("passType"));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("User not found for ad-free pass payment: userId={}", userId);
            return;
        }

        // Check for duplicate
        List<AdFreePass> existing = adFreePassRepository.findByStripePaymentIntentId(paymentIntentId);
        if (!existing.isEmpty()) {
            log.warn("Duplicate payment intent for ad-free pass: {}", paymentIntentId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int days = passType == AdFreePassType.PURCHASE_7D ? 7 : 30;

        AdFreePass pass = AdFreePass.builder()
                .user(user)
                .type(passType)
                .startsAt(now)
                .expiresAt(now.plusDays(days))
                .stripePaymentIntentId(paymentIntentId)
                .build();
        adFreePassRepository.save(pass);

        auditLogService.log(user.getUsername(), AuditActionCodes.AD_FREE_PURCHASE, "AdFreePass", pass.getId(),
                "type=" + passType + " days=" + days);
        log.info("Ad-free pass created: user={}, type={}, expires={}", user.getUsername(), passType, pass.getExpiresAt());
    }

    private void handleDonationPayment(Map<String, String> metadata, Session session) {
        Long userId = Long.parseLong(metadata.get("userId"));
        DonationType donationType = DonationType.valueOf(metadata.get("donationType"));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("User not found for donation: userId={}", userId);
            return;
        }

        int amountCents = session.getAmountTotal() != null ? session.getAmountTotal().intValue() : 0;
        String currency = session.getCurrency() != null ? session.getCurrency() : "usd";

        Donation donation = Donation.builder()
                .user(user)
                .amountCents(amountCents)
                .currency(currency)
                .donationType(donationType)
                .stripePaymentIntentId(session.getPaymentIntent())
                .stripeSubscriptionId(session.getSubscription())
                .status(DonationStatus.COMPLETED)
                .build();
        donationRepository.save(donation);

        // Mark as supporter (one-time >= $5 or any recurring)
        if (donationType == DonationType.RECURRING || amountCents >= 500) {
            user.markAsSupporter();
            userRepository.save(user);
        }

        auditLogService.log(user.getUsername(), AuditActionCodes.DONATION_CREATE, "Donation", donation.getId(),
                "amount=" + amountCents + " type=" + donationType);
        log.info("Donation recorded: user={}, amount={}c, type={}", user.getUsername(), amountCents, donationType);
    }

    @Transactional
    public void refundPayment(String paymentIntentId) {
        initStripe();
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .build();
            Refund.create(params);

            // Mark pass as refunded
            List<AdFreePass> passes = adFreePassRepository.findByStripePaymentIntentId(paymentIntentId);
            for (AdFreePass pass : passes) {
                pass.markRefunded();
                auditLogService.log("admin", AuditActionCodes.AD_FREE_REFUND, "AdFreePass", pass.getId(),
                        "paymentIntent=" + paymentIntentId);
            }

            // Mark donation as refunded
            donationRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(donation -> {
                donation.markRefunded();
                auditLogService.log("admin", AuditActionCodes.DONATION_REFUND, "Donation", donation.getId(),
                        "paymentIntent=" + paymentIntentId);
            });
        } catch (StripeException e) {
            log.error("Stripe refund failed", e);
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }
    }
}
