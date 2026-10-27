package com.woori.wonnie.calendar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Profile("google-oidc")
class GoogleCalendarProvider implements CalendarProvider {
    private static final String EVENTS = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
    private final OAuth2AuthorizedClientManager clients;
    private final RestClient http;
    private final ObjectMapper json;
    private final String webhookUrl;

    GoogleCalendarProvider(
            OAuth2AuthorizedClientManager clients,
            ObjectMapper json,
            @Value("${app.calendar.webhook-url:}") String webhookUrl
    ) {
        this.clients = clients;
        this.json = json;
        this.webhookUrl = webhookUrl;
        this.http = RestClient.create();
    }

    @Override
    public List<CalendarEventSnapshot> events(String principalName, LocalDate from, LocalDate toExclusive) {
        var token = accessToken(principalName);
        var result = new ArrayList<CalendarEventSnapshot>();
        String pageToken = null;
        do {
            var builder = UriComponentsBuilder.fromUriString(EVENTS)
                    .queryParam("timeMin", from.atStartOfDay(BUSINESS_ZONE).toInstant())
                    .queryParam("timeMax", toExclusive.atStartOfDay(BUSINESS_ZONE).toInstant())
                    .queryParam("singleEvents", true)
                    .queryParam("showDeleted", false)
                    .queryParam("maxResults", 2500);
            if (pageToken != null) builder.queryParam("pageToken", pageToken);
            String body;
            try {
                body = http.get().uri(builder.build(true).toUri())
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve().body(String.class);
            } catch (HttpClientErrorException.Unauthorized error) {
                throw new CalendarAuthorizationRequiredException(error);
            }
            try {
                var root = json.readTree(body);
                for (var event : root.path("items")) {
                    var start = eventDate(event.path("start"));
                    var end = eventDate(event.path("end"));
                    if (start == null) continue;
                    if (end == null) end = start;
                    if (event.path("end").has("date") && end.isAfter(start)) end = end.minusDays(1);
                    result.add(new CalendarEventSnapshot(
                            event.path("id").asText(), event.path("summary").asText(""), start, end));
                }
                pageToken = root.path("nextPageToken").asText(null);
            } catch (Exception error) {
                throw new IllegalStateException("Google Calendar 응답을 해석하지 못했어요.", error);
            }
        } while (pageToken != null && !pageToken.isBlank());
        return result;
    }

    @Override
    public WatchRegistration watch(
            String principalName, String channelId, String channelToken, LocalDateTime expiresAt
    ) {
        if (webhookUrl.isBlank()) return null;
        String body;
        try {
            body = http.post().uri(URI.create(EVENTS + "/watch"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken(principalName)))
                    .body(Map.of(
                            "id", channelId,
                            "type", "web_hook",
                            "address", webhookUrl,
                            "token", channelToken,
                            "expiration", expiresAt.toInstant(ZoneOffset.UTC).toEpochMilli()))
                    .retrieve().body(String.class);
        } catch (HttpClientErrorException.Unauthorized error) {
            throw new CalendarAuthorizationRequiredException(error);
        }
        try {
            var root = json.readTree(body);
            var expiration = root.path("expiration").asLong(0);
            return new WatchRegistration(root.path("resourceId").asText(), expiration > 0
                    ? LocalDateTime.ofInstant(Instant.ofEpochMilli(expiration), ZoneOffset.UTC) : expiresAt);
        } catch (Exception error) {
            throw new IllegalStateException("Google Calendar watch 응답을 해석하지 못했어요.", error);
        }
    }

    private String accessToken(String principalName) {
        var request = OAuth2AuthorizeRequest.withClientRegistrationId("google")
                .principal(principalName).build();
        var client = clients.authorize(request);
        if (client == null || client.getAccessToken() == null) {
            throw new CalendarAuthorizationRequiredException();
        }
        return client.getAccessToken().getTokenValue();
    }

    private static LocalDate eventDate(JsonNode node) {
        var date = node.path("date").asText(null);
        if (date != null) return LocalDate.parse(date);
        var dateTime = node.path("dateTime").asText(null);
        return dateTime == null ? null : OffsetDateTime.parse(dateTime).toLocalDate();
    }
}
