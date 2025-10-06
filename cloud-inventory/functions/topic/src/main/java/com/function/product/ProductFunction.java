package com.function.product;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.function.shared.EventBus;

import java.util.*;

public class ProductFunction {

    // Backing store en memoria compartido
    private static final List<Product> products = com.function.shared.Store.PRODUCTS;

    // Modelo
    public static class Product {
        private String id;
        private String name;
        private String description;
        private double price;

        public Product() { }

        public Product(String id, String name, String description, double price) {
            this.id = id; this.name = name; this.description = description; this.price = price;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
    }

    // GET /api/products
    @FunctionName("getProducts")
    public HttpResponseMessage getProducts(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET }, route = "products")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(products)
                .build();
    }

    // GET /api/products/{id}
    @FunctionName("getProductById")
    public HttpResponseMessage getProductById(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET }, route = "products/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        Product p = products.stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (p == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body("No se encuentra el producto " + id)
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(p)
                .build();
    }

    // POST /api/products
    @FunctionName("createProduct")
    public HttpResponseMessage createProduct(
            @HttpTrigger(name = "req", methods = { HttpMethod.POST }, route = "products")
            HttpRequestMessage<Optional<Product>> request,
            final ExecutionContext context) {

        Product product = request.getBody().orElse(null);
        if (product == null || product.getId() == null || product.getName() == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Producto debe tener id y nombre")
                    .build();
        }

        boolean exists = products.stream().anyMatch(p -> p.getId().equals(product.getId()));
        if (exists) {
            return request.createResponseBuilder(HttpStatus.CONFLICT)
                    .body("Producto " + product.getId() + " ya existe")
                    .build();
        }

        products.add(product);

        try {
            EventBus.publish("/product/" + product.getId(), "Inventory.ProductCreated", product);
            context.getLogger().info("Evento ProductCreated emitido");
        } catch (Exception e) {
            context.getLogger().warning("No se pudo publicar evento: " + e.getMessage());
        }

        return request.createResponseBuilder(HttpStatus.CREATED)
                .header("Content-Type", "application/json")
                .body(product)
                .build();
    }

    // PUT /api/products/{id}
    @FunctionName("updateProduct")
    public HttpResponseMessage updateProduct(
            @HttpTrigger(name = "req", methods = { HttpMethod.PUT }, route = "products/{id}")
            HttpRequestMessage<Optional<Product>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        Product updated = request.getBody().orElse(null);
        if (updated == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid body")
                    .build();
        }

        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getId().equals(id)) {
                products.set(i, updated);

                try {
                    EventBus.publish("/product/" + id, "Inventory.ProductUpdated", updated);
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
                .body("No se encuentra el producto " + id)
                .build();
    }

    // DELETE /api/products/{id}
    @FunctionName("deleteProduct")
    public HttpResponseMessage deleteProduct(
            @HttpTrigger(name = "req", methods = { HttpMethod.DELETE }, route = "products/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        boolean removed = products.removeIf(p -> p.getId().equals(id));
        if (!removed) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body("No se encuentra el producto " + id)
                    .build();
        }

        try {
            EventBus.publish("/product/" + id, "Inventory.ProductDeleted", Map.of("id", id));
        } catch (Exception e) {
            context.getLogger().warning("No se pudo publicar evento: " + e.getMessage());
        }

        return request.createResponseBuilder(HttpStatus.NO_CONTENT).build();
    }
}
