package com.example.bff.graphql;

import com.example.bff.model.Warehouse;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

@Controller
public class WarehouseGraphQLController {

    private final WebClient webClient = WebClient.builder().build();

    @QueryMapping
    public Mono<Warehouse> getWarehouseById(@Argument String id) {
        String url = "https://almacen-plan.azurewebsites.net/api/warehouses/" + id + "?code=n4PxululLpiz3X26xIGZFOf4YwZPDIQFfGcZKw0SgRh4AzFuJdC6Rw==";
        return webClient.get().uri(url).retrieve().bodyToMono(Warehouse.class);
    }

    @QueryMapping
    public Mono<List<Warehouse>> getAllWarehouses() {
        String url = "https://almacen-plan.azurewebsites.net/api/warehouses?code=U8PZgudQmG0c9zhDdzFzZJoSioQbfnqdEA0Q3UwKidAzAzFugUNq_Q==";
        return webClient.get().uri(url).retrieve().bodyToMono(new ParameterizedTypeReference<List<Warehouse>>() {});
    }

    @MutationMapping
    public Mono<Warehouse> createWarehouse(@Argument String id, @Argument String name, @Argument String location) {
        Warehouse w = new Warehouse(id, name, location);
        String url = "https://almacen-plan.azurewebsites.net/api/warehouses?code=PJklmpsRD4fIZ2S-EzyHYJzqB0wjcnFIYgJ7Q8FqNvf_AzFu7AFwog==";
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(w)
                .retrieve()
                .bodyToMono(Warehouse.class)
                .onErrorResume(e -> Mono.empty());
    }
}
