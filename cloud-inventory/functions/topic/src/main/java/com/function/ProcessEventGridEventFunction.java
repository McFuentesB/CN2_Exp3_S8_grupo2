package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.logging.Logger;   // <-- este import

public class ProcessEventGridEventFunction {

  @FunctionName("ProcessEventGridEvent")
  public void run(
      @EventGridTrigger(name = "eventGridEvent") String content,
      final ExecutionContext context) {

    Logger logger = context.getLogger();   // <-- sin cast a slf4j
    logger.info("Funcion con Event Grid trigger ejecutada.");
    logger.info("Payload recibido: " + content);

    try {
      Gson gson = new Gson();
      JsonObject obj = gson.fromJson(content, JsonObject.class);

      String eventType = obj.has("eventType") ? obj.get("eventType").getAsString() : "Unknown";
      String subject   = obj.has("subject")   ? obj.get("subject").getAsString()   : "Unknown";
      String dataStr   = obj.has("data")      ? obj.get("data").toString()         : "{}";

      logger.info("Tipo de evento: " + eventType);
      logger.info("Subject: " + subject);
      logger.info("Data: " + dataStr);
    } catch (Exception e) {
      logger.severe("Error procesando evento: " + e.getMessage());
      throw e; // para que Event Grid reintente si corresponde
    }
  }
}
