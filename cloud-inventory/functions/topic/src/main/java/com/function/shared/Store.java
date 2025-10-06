package com.function.shared;

import com.function.product.ProductFunction;
import com.function.warehouse.WarehouseFunction;

import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/** Estado en memoria (solo demo) compartido entre REST y GraphQL */
public final class Store {
  private Store(){}

  public static final List<ProductFunction.Product> PRODUCTS = new CopyOnWriteArrayList<>();
  public static final List<WarehouseFunction.Warehouse> WAREHOUSES = new CopyOnWriteArrayList<>();
  public static final Set<Edge> EDGES = ConcurrentHashMap.newKeySet(); // productId -> warehouseId

  public static final class Edge {
    public String productId;
    public String warehouseId;
    public Edge() {}
    public Edge(String p, String w){ this.productId = p; this.warehouseId = w; }
    @Override public int hashCode(){ return Objects.hash(productId, warehouseId); }
    @Override public boolean equals(Object o){
      if(!(o instanceof Edge e)) return false;
      return Objects.equals(productId, e.productId) && Objects.equals(warehouseId, e.warehouseId);
    }
  }
}
