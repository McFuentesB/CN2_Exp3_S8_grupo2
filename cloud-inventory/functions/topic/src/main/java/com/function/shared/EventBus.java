package com.function.shared;

import com.azure.messaging.eventgrid.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;

public class EventBus {
  private static volatile EventGridPublisherClient<EventGridEvent> client;

  private static EventGridPublisherClient<EventGridEvent> getClient() {
    if (client == null) {
      synchronized (EventBus.class) {
        if (client == null) {
          String endpoint = System.getenv("EVENTGRID_TOPIC_ENDPOINT");
          String key = System.getenv("EVENTGRID_TOPIC_KEY");
          if (endpoint == null || key == null) {
            throw new IllegalStateException("Faltan EVENTGRID_TOPIC_ENDPOINT/EVENTGRID_TOPIC_KEY");
          }
          client = new EventGridPublisherClientBuilder()
              .endpoint(endpoint)
              .credential(new AzureKeyCredential(key))
              .buildEventGridEventPublisherClient();
        }
      }
    }
    return client;
  }

  public static void publish(String subject, String eventType, Object data) {
    EventGridEvent ev = new EventGridEvent(subject, eventType, BinaryData.fromObject(data), "0.1");
    getClient().sendEvent(ev);
  }
}
