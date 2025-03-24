package com.example.backend.controller;

import com.example.backend.service.CreditService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    private final CreditService creditService;

    public PaymentController(CreditService creditService) {
        this.creditService = creditService;
        Stripe.apiKey = stripeSecretKey;         // Ustawienie klucza API raz
    //    Stripe.apiVersion = "2025-02-24.acacia"; // Ustawienie wersji API raz
    }

    // ✅ Tworzenie PaymentIntent
    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            if (username == null || username.isEmpty()) {
                username = "unknown_user";
            }

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount((Long) request.get("amount"))
                    .setCurrency("pln")
                    .addPaymentMethodType("card")
                    .putMetadata("username", username)
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Webhook do obsługi płatności
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // Weryfikacja podpisu webhooka
            Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);

        //    System.out.println("🔹 Pełna treść webhooka: " + payload);
            System.out.println("🔹 Typ zdarzenia: " + event.getType());
            if ("payment_intent.succeeded".equals(event.getType())) {
                System.out.println("🔹 Płatność zakończona powodzeniem");
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);

                if (paymentIntent != null) {
                 //   System.out.println("🔹 paymentIntent != null"); // Poprawiono literówkę
                    String paymentId = paymentIntent.getId();
                    String username = paymentIntent.getMetadata().get("username");
                    Long amountPaid = paymentIntent.getAmount();
                    Long packageId = Long.parseLong(paymentIntent.getMetadata().get("package_id"));
                    Long userId = Long.parseLong(paymentIntent.getMetadata().get("user_id"));

                    System.out.println("🔹 PaymentIntent ID: " + paymentId);
                    System.out.println("🔹 Kwota zapłacona: " + amountPaid + " groszy (PLN)");
                    System.out.println("🔹 Użytkownik: " + username);
                    System.out.println("🔹 User ID: " + userId);
                    System.out.println("🔹 Package ID: " + packageId);

                    if (username != null) {
                        // creditService.addCreditsToUser(username, 10);
                        creditService.assignCredits( userId,  packageId, paymentId);
                        System.out.println(
                                "✅ Płatność zakończona sukcesem! ID: " + paymentId + ", Użytkownik: " + username);
                    } else {
                        System.out.println("⚠️ Brak username w metadanych – nie można dodać tokenów.");
                    }
                } else {
                    System.out.println("❌ paymentIntent jest null"); // Dodano log dla debugowania
                }
            }

            return ResponseEntity.ok("✅ Webhook received");

        } catch (Exception e) {
            System.out.println("❌ Błąd obsługi webhooka: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("⚠️ Webhook error: " + e.getMessage());
        }
    }
}