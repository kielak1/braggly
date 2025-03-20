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
    private String stripeWebhookSecret; // 🔹 Klucz webhooka

    private final CreditService creditService;

    public PaymentController(CreditService creditService) {
        this.creditService = creditService;
    }

    // ✅ Tworzenie PaymentIntent
    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody Map<String, Object> request) {
        Stripe.apiKey = stripeSecretKey;

        try {
            String userEmail = (String) request.get("email"); // Pobranie e-maila użytkownika
            Long amount = ((Number) request.get("amount")).longValue(); // Poprawna obsługa konwersji

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount) // Kwota w groszach (np. 1000 = 10 PLN)
                    .setCurrency("pln")
                    .addPaymentMethodType("card")
                    .putMetadata("email", userEmail) // Przekazanie e-maila w metadanych
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "⚠️ Stripe error: " + e.getMessage()));
        }
    }

    // ✅ Webhook do obsługi płatności
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // Weryfikacja podpisu webhooka
            Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);

            System.out.println("🔹 Pełna treść webhooka: " + payload); // 🔥 Pełne logowanie webhooka!

            if ("payment_intent.succeeded".equals(event.getType())) {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);

                if (paymentIntent != null) {
                    String paymentId = paymentIntent.getId();
                    String username = paymentIntent.getMetadata().get("username"); // Pobranie username
                    Long amountPaid = paymentIntent.getAmount(); // Pobranie kwoty

                    System.out.println("🔹 PaymentIntent ID: " + paymentId);
                    System.out.println("🔹 Kwota zapłacona: " + amountPaid + " groszy (PLN)");
                    System.out.println("🔹 Użytkownik: " + username);

                    if (username != null) {
                    //    creditService.addCreditsToUser(username, 10);
                        System.out.println(
                                "✅ Płatność zakończona sukcesem! ID: " + paymentId + ", Użytkownik: " + username);
                    } else {
                        System.out.println("⚠️ Brak username w metadanych – nie można dodać tokenów.");
                    }
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