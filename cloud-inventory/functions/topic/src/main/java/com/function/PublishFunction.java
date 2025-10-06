package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import java.util.*;

/**
 * POST /api/publish
 * Body JSON libre; lo manda como data dentro del EventGridEvent.
 */
public class PublishFunction {

    @FunctionName("HttpTrigger-Publish")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION)
        HttpRequestMessage<Optional<Map<String, Object>>> request,
        final ExecutionContext context
    ) {
        var log = context.getLogger();
        String topicEndpoint = System.getenv("EVENTGRID_TOPIC_ENDPOINT"); // ej: https://<topic>.<region>-1.eventgrid.azure.net/api/events
        String topicKey      = System.getenv("EVENTGRID_TOPIC_KEY");

        if (topicEndpoint == null || topicKey == null) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("ok", false, "error", "Faltan EVENTGRID_TOPIC_ENDPOINT o EVENTGRID_TOPIC_KEY"))
                .build();
        }

        try {
            Map<String, Object> payload = request.getBody().orElseGet(HashMap::new);

            // eventType/subject: define tu convención
            String eventType = String.valueOf(payload.getOrDefault("eventType", "Inventory.Generic"));
            String subject   = String.valueOf(payload.getOrDefault("subject", "/inventory/generic"));
            String dataVersion = "0.1";

            // data: todo el body
            BinaryData data = BinaryData.fromObject(payload);

            EventGridEvent egEvent = new EventGridEvent(subject, eventType, data, dataVersion);

            EventGridPublisherClient<EventGridEvent> client =
                new EventGridPublisherClientBuilder()
                    .endpoint(topicEndpoint)
                    .credential(new AzureKeyCredential(topicKey))
                    .buildEventGridEventPublisherClient();

            client.sendEvent(egEvent); // también existe sendEvents(List.of(...))

            log.info("Evento publicado: " + eventType + " " + subject);
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(Map.of("ok", true, "eventType", eventType, "subject", subject))
                    .build();

        } catch (Exception e) {
            log.severe("Error publicando: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "error", e.getMessage()))
                    .build();
        }
    }
}
