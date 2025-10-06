package com.example.bff.controller;

import com.example.bff.model.Product;
import com.example.bff.model.Warehouse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BffController {

    private final WebClient http;

    public BffController(WebClient.Builder builder) {
        this.http = builder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ---------- URLs de tu Function App (con code= ya incluido) ----------
    private static final String BASE = "https://funcion1-fggvaeb3dsb7f2cc.eastus2-01.azurewebsites.net";
    private static final String BASE2 = "https://funcion2-deerd5evg8cce5gf.eastus2-01.azurewebsites.net";
    // PRODUCTS
    private static final String URL_GET_PRODUCTS     = BASE + "/api/products?code=lJ-ZZDqEwdQRi-a_X-C6PaR-IG0tH9eeQTKStha6cENoAzFuJTPkYg==";
    private static final String URL_GET_PRODUCT_BYID = BASE + "/api/products/{id}?code=lJ-ZZDqEwdQRi-a_X-C6PaR-IG0tH9eeQTKStha6cENoAzFuJTPkYg==";
    private static final String URL_CREATE_PRODUCT   = BASE + "/api/products?code=lJ-ZZDqEwdQRi-a_X-C6PaR-IG0tH9eeQTKStha6cENoAzFuJTPkYg==";
    private static final String URL_UPDATE_PRODUCT   = BASE + "/api/products/{id}?code=lJ-ZZDqEwdQRi-a_X-C6PaR-IG0tH9eeQTKStha6cENoAzFuJTPkYg==";
    private static final String URL_DELETE_PRODUCT   = BASE + "/api/products/{id}?code=lJ-ZZDqEwdQRi-a_X-C6PaR-IG0tH9eeQTKStha6cENoAzFuJTPkYg==";

    // WAREHOUSES
    private static final String URL_GET_WAREHOUSES     = BASE2 + "/api/warehouses?code=SkqQmjeVo_oEVc9b0dlTiZm7u62gSuHOt0GrClIl-hqyAzFuUQzUtQ==";
    private static final String URL_GET_WAREHOUSE_BYID = BASE2 + "/api/warehouses/{id}?code=SkqQmjeVo_oEVc9b0dlTiZm7u62gSuHOt0GrClIl-hqyAzFuUQzUtQ==";
    private static final String URL_CREATE_WAREHOUSE   = BASE2 + "/api/warehouses?code=SkqQmjeVo_oEVc9b0dlTiZm7u62gSuHOt0GrClIl-hqyAzFuUQzUtQ==";
    private static final String URL_UPDATE_WAREHOUSE   = BASE2 + "/api/warehouses/{id}?code=SkqQmjeVo_oEVc9b0dlTiZm7u62gSuHOt0GrClIl-hqyAzFuUQzUtQ==";
    private static final String URL_DELETE_WAREHOUSE   = BASE2 + "/api/warehouses/{id}?code=SkqQmjeVo_oEVc9b0dlTiZm7u62gSuHOt0GrClIl-hqyAzFuUQzUtQ==";

    // GRAPHQL y publisher opcional
    private static final String URL_GRAPHQL            = BASE + "/api/graphql?code=lJ-ZZDqEwdQRi-a_X-C6PaR-IG0tH9eeQTKStha6cENoAzFuJTPkYg==";
    private static final String URL_HTTP_PUBLISH       = BASE + "/api/HttpTrigger-Publish?code=lJ-ZZDqEwdQRi-a_X-C6PaR-IG0tH9eeQTKStha6cENoAzFuJTPkYg==";

    // ------------------ PRODUCTS ------------------

    @GetMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Product>> getProducts() {
        return http.get()
                .uri(URL_GET_PRODUCTS)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Product>>() {});
    }

    @GetMapping(value = "/products/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Product> getProductById(@PathVariable String id) {
        return http.get()
                .uri(URL_GET_PRODUCT_BYID, id)
                .retrieve()
                .bodyToMono(Product.class);
    }

    @PostMapping(value = "/products", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Product> createProduct(@RequestBody Product product) {
        return http.post()
                .uri(URL_CREATE_PRODUCT)
                .bodyValue(product)
                .retrieve()
                .bodyToMono(Product.class);
    }

    @PutMapping(value = "/products/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Product> updateProduct(@PathVariable String id, @RequestBody Product product) {
        return http.put()
                .uri(URL_UPDATE_PRODUCT, id)
                .bodyValue(product)
                .retrieve()
                .bodyToMono(Product.class);
    }

    @DeleteMapping(value = "/products/{id}")
    public Mono<HttpStatus> deleteProduct(@PathVariable String id) {
        return http.delete()
                .uri(URL_DELETE_PRODUCT, id)
                .exchangeToMono(resp -> Mono.just(resp.statusCode().is2xxSuccessful()
                        ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND));
    }

    // ------------------ WAREHOUSES ------------------

    @GetMapping(value = "/warehouses", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Warehouse>> getWarehouses() {
        return http.get()
                .uri(URL_GET_WAREHOUSES)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Warehouse>>() {});
    }

    @GetMapping(value = "/warehouses/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Warehouse> getWarehouseById(@PathVariable String id) {
        return http.get()
                .uri(URL_GET_WAREHOUSE_BYID, id)
                .retrieve()
                .bodyToMono(Warehouse.class);
    }

    @PostMapping(value = "/warehouses", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Warehouse> createWarehouse(@RequestBody Warehouse warehouse) {
        return http.post()
                .uri(URL_CREATE_WAREHOUSE)
                .bodyValue(warehouse)
                .retrieve()
                .bodyToMono(Warehouse.class);
    }

    @PutMapping(value = "/warehouses/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Warehouse> updateWarehouse(@PathVariable String id, @RequestBody Warehouse warehouse) {
        return http.put()
                .uri(URL_UPDATE_WAREHOUSE, id)
                .bodyValue(warehouse)
                .retrieve()
                .bodyToMono(Warehouse.class);
    }

    @DeleteMapping(value = "/warehouses/{id}")
    public Mono<HttpStatus> deleteWarehouse(@PathVariable String id) {
        return http.delete()
                .uri(URL_DELETE_WAREHOUSE, id)
                .exchangeToMono(resp -> Mono.just(resp.statusCode().is2xxSuccessful()
                        ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND));
    }



    @PostMapping(value = "/graphql", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> graphql(@RequestBody Map<String, Object> payload) {
        // payload típico: { "query": "...", "variables": { ... } }
        return http.post()
                .uri(URL_GRAPHQL)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
    }



    @PostMapping(value = "/publish", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map> publish(@RequestBody Map<String,Object> body) {
        // cuerpo libre; tu Function lo envía como data dentro del EventGridEvent
        return http.post()
                .uri(URL_HTTP_PUBLISH)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class);
    }
}
