package com.function.warehouse;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.function.shared.EventBus;

import java.util.*;

public class WarehouseFunction {

    // Backing store en memoria compartido
    private static final List<Warehouse> warehouses = com.function.shared.Store.WAREHOUSES;

    // Modelo
    public static class Warehouse {
        private String id;
        private String name;
        private String location;

        public Warehouse() { }

        public Warehouse(String id, String name, String location) {
            this.id = id; this.name = name; this.location = location;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    // GET /api/warehouses
    @FunctionName("getWarehouses")
    public HttpResponseMessage getWarehouses(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET }, route = "warehouses")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(warehouses)
                .build();
    }

    // GET /api/warehouses/{id}
    @FunctionName("getWarehouseById")
    public HttpResponseMessage getWarehouseById(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET }, route = "warehouses/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        Warehouse w = warehouses.stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (w == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body("No se encuentra el almacen para el id " + id)
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(w)
                .build();
    }

    // POST /api/warehouses
    @FunctionName("createWarehouse")
    public HttpResponseMessage createWarehouse(
            @HttpTrigger(name = "req", methods = { HttpMethod.POST }, route = "warehouses")
            HttpRequestMessage<Optional<Warehouse>> request,
            final ExecutionContext context) {

        Warehouse w = request.getBody().orElse(null);
        if (w == null || w.getId() == null || w.getName() == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Almacen debe tener id y nombre")
                    .build();
        }

        boolean exists = warehouses.stream().anyMatch(x -> x.getId().equals(w.getId()));
        if (exists) {
            return request.createResponseBuilder(HttpStatus.CONFLICT)
                    .body("Almacen con el id " + w.getId() + " ya existe")
                    .build();
        }

        warehouses.add(w);

        try {
            EventBus.publish("/warehouse/" + w.getId(), "Inventory.WarehouseCreated", w);
            context.getLogger().info("Evento WarehouseCreated emitido");
        } catch (Exception e) {
            context.getLogger().warning("No se pudo publicar evento: " + e.getMessage());
        }

        return request.createResponseBuilder(HttpStatus.CREATED)
                .header("Content-Type", "application/json")
                .body(w)
                .build();
    }

    // PUT /api/warehouses/{id}
    @FunctionName("updateWarehouse")
    public HttpResponseMessage updateWarehouse(
            @HttpTrigger(name = "req", methods = { HttpMethod.PUT }, route = "warehouses/{id}")
            HttpRequestMessage<Optional<Warehouse>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        Warehouse updated = request.getBody().orElse(null);
        if (updated == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid body")
                    .build();
        }

        for (int i = 0; i < warehouses.size(); i++) {
            if (warehouses.get(i).getId().equals(id)) {
                warehouses.set(i, updated);

                try {
                    EventBus.publish("/warehouse/" + id, "Inventory.WarehouseUpdated", updated);
                    context.getLogger().info("Evento WarehouseUpdated emitido");
                } catch (Exception e) {
                    context.getLogger().warning("No se pudo publicar evento: " + e.getMessage());
                }

                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(updated)
                        .build();
            }
        }

        return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body("No se encuentra el almacen para el id " + id)
                .build();
    }

    // DELETE /api/warehouses/{id}
    @FunctionName("deleteWarehouse")
    public HttpResponseMessage deleteWarehouse(
            @HttpTrigger(name = "req", methods = { HttpMethod.DELETE }, route = "warehouses/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        boolean removed = warehouses.removeIf(x -> x.getId().equals(id));
        if (!removed) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body("No se encuentra el almacen para el id " + id)
                    .build();
        }

        try {
            EventBus.publish("/warehouse/" + id, "Inventory.WarehouseDeleted", Map.of("id", id));
            context.getLogger().info("Evento WarehouseDeleted emitido");
        } catch (Exception e) {
            context.getLogger().warning("No se pudo publicar evento: " + e.getMessage());
        }

        return request.createResponseBuilder(HttpStatus.NO_CONTENT).build();
    }
}
