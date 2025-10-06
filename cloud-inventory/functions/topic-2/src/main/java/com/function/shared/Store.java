package com.function.shared;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Objects;

public final class Store {
  private Store() {}

  // ------- DTOs (simples, sin dependencia de ProductFunction/WarehouseFunction)
  public static class Product {
    private String id;
    private String name;
    private String description;
    private double price;

    public Product() {}
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

  public static class Warehouse {
    private String id;
    private String name;
    private String location;

    public Warehouse() {}
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

  public static class Edge {
    public String productId;
    public String warehouseId;
    public Edge() {}
    public Edge(String p, String w) { this.productId = p; this.warehouseId = w; }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Edge e)) return false;
      return Objects.equals(productId, e.productId) && Objects.equals(warehouseId, e.warehouseId);
    }
    @Override public int hashCode() { return Objects.hash(productId, warehouseId); }
  }

  // ------- Stores en memoria (thread-safe para demo)
  public static final List<Product> PRODUCTS = new CopyOnWriteArrayList<>();
  public static final List<Warehouse> WAREHOUSES = new CopyOnWriteArrayList<>();
  public static final List<Edge> EDGES = new CopyOnWriteArrayList<>();
}
