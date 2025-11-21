package com.example.emergency;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class SmsWebhookController {

    // IMPORTANT — your public ngrok HTTPS URL
    private static final String PUBLIC_BASE_URL = "https://ed4dea27845f.ngrok-free.app";

    // Thread-safe event store
    private final Map<String, EmergencyEvent> events = new ConcurrentHashMap<>();

    // Escape XML characters safely for Twilio
    private String xmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // ------------------------------
    // 1. TWILIO → Your server (incoming SMS)
    // ------------------------------
    @PostMapping(value = "/sms/webhook", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> receiveSms(
            @RequestParam("From") String from,
            @RequestParam("Body") String body
    ) {
        try {
            System.out.println("---- Incoming SMS ----");
            System.out.println("FROM: " + from);
            System.out.println("BODY: " + body);

            String standardized = body != null ? body.toUpperCase() : "";
            String issue = "";
            String location = "Not specified";

            if (standardized.contains("LOC:")) {
                String[] parts = standardized.split("LOC:");
                issue = parts[0].replace("EMERGENCY:", "").trim();
                location = parts.length > 1 ? parts[1].trim() : "Unknown";
            } else {
                issue = body.trim();
            }

            // Build emergency event
            EmergencyEvent event = new EmergencyEvent();
            event.setSenderPhone(from);
            event.setIssue(issue);
            event.setRoughLocation(location);

            events.put(from, event);

            System.out.println("Event stored: " + event);

            // Encode phone for URL
            String encodedSender = URLEncoder.encode(from, StandardCharsets.UTF_8);
            String link = PUBLIC_BASE_URL + "/gps.html?sender=" + encodedSender;

            // Twilio auto-reply message
            String replyMsg =
                    "Emergency report (" + issue +
                    "). Tap this link to share your GPS location: " +
                    link;

            String twiml =
                    "<Response><Message>" +
                            xmlEscape(replyMsg) +
                            "</Message></Response>";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(twiml);

        } catch (Exception e) {
            System.err.println("ERROR in /sms/webhook: " + e.getMessage());

            String twiml =
                    "<Response><Message>Error processing emergency request.</Message></Response>";

            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(twiml);
        }
    }

    // ------------------------------
    // 2. Browser → Your server after clicking GPS link
    // ------------------------------
    @PostMapping("/receive-gps")
    public ResponseEntity<String> receiveGps(
            @RequestParam("sender") String sender,
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon
    ) {
        System.out.println("---- GPS Post Received ----");
        System.out.println("Sender: " + sender);
        System.out.println("Lat/Lon: " + lat + ", " + lon);

        EmergencyEvent event = events.get(sender);

        if (event == null) {
            return ResponseEntity.badRequest().body("Error: Event not found for sender.");
        }

        event.setLatitude(lat);
        event.setLongitude(lon);

        System.out.println("Event updated: " + event);

        return ResponseEntity.ok("Location received successfully.");
    }

    // Debug endpoint
    @GetMapping("/event")
    public EmergencyEvent getEvent(@RequestParam("sender") String sender) {
        return events.get(sender);
    }
}
