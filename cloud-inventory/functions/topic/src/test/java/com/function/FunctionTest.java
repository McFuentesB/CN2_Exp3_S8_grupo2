package com.function;



import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class FunctionTest {
  interface PublisherFactory {
    com.azure.messaging.eventgrid.EventGridPublisherClient<com.azure.messaging.eventgrid.EventGridEvent> create();
  }

  private final PublisherFactory factory;

  public FunctionTest() {
    this.factory = () -> new com.azure.messaging.eventgrid.EventGridPublisherClientBuilder()
        .endpoint(System.getenv("EVENTGRID_TOPIC_ENDPOINT"))
        .credential(new com.azure.core.credential.AzureKeyCredential(System.getenv("EVENTGRID_TOPIC_KEY")))
        .buildEventGridEventPublisherClient();
  }

  // para tests
  FunctionTest(PublisherFactory factory) { this.factory = factory; }

  @FunctionName("HttpTrigger-Publish")
  public HttpResponseMessage run(@HttpTrigger(name="req",methods={HttpMethod.POST},authLevel=AuthorizationLevel.FUNCTION)
                                 HttpRequestMessage<Optional<Map<String,Object>>> request,
                                 final ExecutionContext context) {
    var log = context.getLogger();
    try {
      Map<String,Object> body = request.getBody().orElseGet(HashMap::new);
      var client = factory.create();
      var ev = new com.azure.messaging.eventgrid.EventGridEvent(
          String.valueOf(body.getOrDefault("subject","/inventory/generic")),
          String.valueOf(body.getOrDefault("eventType","Inventory.Generic")),
          com.azure.core.util.BinaryData.fromObject(body),
          "0.1");
      client.sendEvent(ev);
      return request.createResponseBuilder(HttpStatus.OK).body(Map.of("ok",true)).build();
    } catch (Exception e) {
      log.severe(e.getMessage());
      return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("ok",false)).build();
    }
  }
}
