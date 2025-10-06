package com.example.bff.graphql;

import com.example.bff.model.Product;
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
public class ProductGraphQLController {

    private final WebClient webClient = WebClient.builder().build();

    @QueryMapping
    public Mono<Product> getProductById(@Argument String id) {
        String url = "https://producto-plan.azurewebsites.net/api/products/" + id + "?code=6zbseQRT80_Vl5jZ1ptCXHp70XY19rt1e3Ekx9u9ONcmAzFuUI25-Q==";
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Product.class);
    }

    @QueryMapping
    public Mono<List<Product>> getAllProducts() {
        String url = "https://producto-plan.azurewebsites.net/api/products?code=1BTxKaA75iXujiE5Ao7o4HZUoF6NviI6CMNYrfoKN7CzAzFu7zpzvQ==";
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Product>>() {});
    }

    @MutationMapping
    public Mono<Product> createProduct(@Argument String id, @Argument String name, @Argument double price, @Argument int stock) {
        Product p = new Product(id, name, price, stock);
        String url = "https://producto-plan.azurewebsites.net/api/products?code=icuTG8Gd8WXGzNPDcQ9qwHs6HCF3JdBoSd5-S_xmkvAhAzFu6fNPwA==";
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(p)
                .retrieve()
                .bodyToMono(Product.class)
                .onErrorResume(e -> Mono.empty());
    }
}
