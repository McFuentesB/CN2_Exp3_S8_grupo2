package com.function.graphql;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import com.function.shared.Store;
import com.function.shared.EventBus;
import com.function.product.ProductFunction;
import com.function.warehouse.WarehouseFunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import graphql.GraphQL;
import graphql.schema.idl.*;
import graphql.schema.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * POST /api/graphql
 * Body: { "query": "...", "variables": { ... } }
 */
public class GraphQLFunction {

  // ---------- Esquema SDL (simple y suficiente para la demo) ----------
  private static final String SDL = """
    type Product { id: String!, name: String!, description: String, price: Float! }
    type Warehouse { id: String!, name: String!, location: String }
    type Edge { productId: String!, warehouseId: String! }
    type Graph { products: [Product!]!, warehouses: [Warehouse!]!, edges: [Edge!]! }

    input ProductInput { id: String!, name: String!, description: String, price: Float! }
    input WarehouseInput { id: String!, name: String!, location: String }

    type Query {
      products: [Product!]!
      product(id: String!): Product
      warehouses: [Warehouse!]!
      warehouse(id: String!): Warehouse
      graph: Graph!
    }

    type Mutation {
      createProduct(input: ProductInput!): Product!
      updateProduct(id: String!, input: ProductInput!): Product!
      deleteProduct(id: String!): Boolean!

      createWarehouse(input: WarehouseInput!): Warehouse!
      updateWarehouse(id: String!, input: WarehouseInput!): Warehouse!
      deleteWarehouse(id: String!): Boolean!

      link(productId: String!, warehouseId: String!): Boolean!
      unlink(productId: String!, warehouseId: String!): Boolean!
    }

    schema { query: Query, mutation: Mutation }
  """;

  private static volatile GraphQL GRAPHQL;
  private static final ObjectMapper JSON = new ObjectMapper();

  private static GraphQL graphQL() {
    if (GRAPHQL == null) {
      synchronized (GraphQLFunction.class) {
        if (GRAPHQL == null) {
          TypeDefinitionRegistry registry = new SchemaParser().parse(SDL);

          RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()

            // ------- Query -------
            .type(TypeRuntimeWiring.newTypeWiring("Query")
              .dataFetcher("products", env -> Store.PRODUCTS)
              .dataFetcher("product", env -> {
                String id = env.getArgument("id");
                return Store.PRODUCTS.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
              })
              .dataFetcher("warehouses", env -> Store.WAREHOUSES)
              .dataFetcher("warehouse", env -> {
                String id = env.getArgument("id");
                return Store.WAREHOUSES.stream().filter(w -> w.getId().equals(id)).findFirst().orElse(null);
              })
              .dataFetcher("graph", env -> Map.of(
                  "products", Store.PRODUCTS,
                  "warehouses", Store.WAREHOUSES,
                  "edges", Store.EDGES))
            )

            // ------- Mutation -------
            .type(TypeRuntimeWiring.newTypeWiring("Mutation")
              .dataFetcher("createProduct", env -> {
                Map<String,Object> in = env.getArgument("input");
                ProductFunction.Product p = mapToProduct(in);
                // validación mínima
                if (p.getId()==null || p.getName()==null) throw new RuntimeException("id y name son requeridos");
                boolean exists = Store.PRODUCTS.stream().anyMatch(x -> x.getId().equals(p.getId()));
                if (exists) throw new RuntimeException("Producto ya existe: "+p.getId());
                Store.PRODUCTS.add(p);
                try { EventBus.publish("/product/"+p.getId(), "Inventory.ProductCreated", p); } catch (Exception ignore) {}
                return p;
              })
              .dataFetcher("updateProduct", env -> {
                String id = env.getArgument("id");
                Map<String,Object> in = env.getArgument("input");
                ProductFunction.Product upd = mapToProduct(in);

                for (int i=0;i<Store.PRODUCTS.size();i++) {
                  if (Store.PRODUCTS.get(i).getId().equals(id)) {
                    Store.PRODUCTS.set(i, upd);
                    try { EventBus.publish("/product/"+id, "Inventory.ProductUpdated", upd); } catch (Exception ignore) {}
                    return upd;
                  }
                }
                throw new RuntimeException("No existe producto: "+id);
              })
              .dataFetcher("deleteProduct", env -> {
                String id = env.getArgument("id");
                boolean removed = Store.PRODUCTS.removeIf(p -> p.getId().equals(id));
                if (removed) { try { EventBus.publish("/product/"+id, "Inventory.ProductDeleted", Map.of("id", id)); } catch (Exception ignore) {} }
                return removed;
              })

              .dataFetcher("createWarehouse", env -> {
                Map<String,Object> in = env.getArgument("input");
                WarehouseFunction.Warehouse w = mapToWarehouse(in);
                if (w.getId()==null || w.getName()==null) throw new RuntimeException("id y name son requeridos");
                boolean exists = Store.WAREHOUSES.stream().anyMatch(x -> x.getId().equals(w.getId()));
                if (exists) throw new RuntimeException("Almacén ya existe: "+w.getId());
                Store.WAREHOUSES.add(w);
                try { EventBus.publish("/warehouse/"+w.getId(), "Inventory.WarehouseCreated", w); } catch (Exception ignore) {}
                return w;
              })
              .dataFetcher("updateWarehouse", env -> {
                String id = env.getArgument("id");
                Map<String,Object> in = env.getArgument("input");
                WarehouseFunction.Warehouse upd = mapToWarehouse(in);

                for (int i=0;i<Store.WAREHOUSES.size();i++) {
                  if (Store.WAREHOUSES.get(i).getId().equals(id)) {
                    Store.WAREHOUSES.set(i, upd);
                    try { EventBus.publish("/warehouse/"+id, "Inventory.WarehouseUpdated", upd); } catch (Exception ignore) {}
                    return upd;
                  }
                }
                throw new RuntimeException("No existe almacén: "+id);
              })
              .dataFetcher("deleteWarehouse", env -> {
                String id = env.getArgument("id");
                boolean removed = Store.WAREHOUSES.removeIf(w -> w.getId().equals(id));
                if (removed) { try { EventBus.publish("/warehouse/"+id, "Inventory.WarehouseDeleted", Map.of("id", id)); } catch (Exception ignore) {} }
                // borra edges del grafo
                Store.EDGES.removeIf(e -> id.equals(e.warehouseId));
                return removed;
              })

              .dataFetcher("link", env -> {
                String productId = env.getArgument("productId");
                String warehouseId = env.getArgument("warehouseId");
                boolean ok = Store.EDGES.add(new Store.Edge(productId, warehouseId));
                if (ok) { try { EventBus.publish("/graph/link", "Inventory.GraphLinked", Map.of("productId", productId, "warehouseId", warehouseId)); } catch (Exception ignore) {} }
                return ok;
              })
              .dataFetcher("unlink", env -> {
                String productId = env.getArgument("productId");
                String warehouseId = env.getArgument("warehouseId");
                boolean ok = Store.EDGES.remove(new Store.Edge(productId, warehouseId));
                if (ok) { try { EventBus.publish("/graph/unlink", "Inventory.GraphUnlinked", Map.of("productId", productId, "warehouseId", warehouseId)); } catch (Exception ignore) {} }
                return ok;
              })
            )
            .build();

          GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(registry, wiring);
          GRAPHQL = GraphQL.newGraphQL(schema).build();
        }
      }
    }
    return GRAPHQL;
  }

  private static ProductFunction.Product mapToProduct(Map<String,Object> in){
    ProductFunction.Product p = new ProductFunction.Product();
    p.setId(String.valueOf(in.get("id")));
    p.setName(String.valueOf(in.get("name")));
    p.setDescription(in.get("description") == null ? null : String.valueOf(in.get("description")));
    Object price = in.get("price");
    p.setPrice(price==null?0.0: Double.valueOf(String.valueOf(price)));
    return p;
  }

  private static WarehouseFunction.Warehouse mapToWarehouse(Map<String,Object> in){
    WarehouseFunction.Warehouse w = new WarehouseFunction.Warehouse();
    w.setId(String.valueOf(in.get("id")));
    w.setName(String.valueOf(in.get("name")));
    w.setLocation(in.get("location")==null? null : String.valueOf(in.get("location")));
    return w;
  }

  // ------------------ Azure Function ------------------
  @FunctionName("GraphQL")
  public HttpResponseMessage handle(
      @HttpTrigger(name="req", methods={HttpMethod.POST}, route="graphql")
      HttpRequestMessage<Optional<String>> request,
      final ExecutionContext context) {

    try {
      String body = request.getBody().orElse("{}");
      Map<String,Object> payload = JSON.readValue(body, new TypeReference<>() {});
      String query = (String) payload.getOrDefault("query", "");
      @SuppressWarnings("unchecked")
      Map<String,Object> variables = (Map<String,Object>) payload.getOrDefault("variables", Collections.emptyMap());

      var result = graphQL().execute(builder -> builder.query(query).variables(variables));

      Map<String,Object> resp = new LinkedHashMap<>();
      if (result.getErrors()!=null && !result.getErrors().isEmpty()) {
        resp.put("errors", result.getErrors().stream().map(e -> Map.of("message", e.getMessage())).collect(Collectors.toList()));
      }
      resp.put("data", result.getData());

      return request.createResponseBuilder(HttpStatus.OK)
          .header("Content-Type","application/json")
          .body(JSON.writeValueAsString(resp))
          .build();

    } catch (Exception e) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .header("Content-Type","application/json")
          .body(Map.of("errors", List.of(Map.of("message", e.getMessage()))))
          .build();
    }
  }
}
