package se.curity.examples.spark;

import se.curity.examples.products.Product;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of the product service that uses a hash map to store the products
 */
public class ProductServiceMapImpl implements ProductService {

    private final ConcurrentHashMap<String, Product> productMap;


    public ProductServiceMapImpl() {
        productMap = createProductList();
    }

    @Override
    public Product getProduct(String id) {
        return productMap.get(id);
    }

    @Override
    public Collection<Product> getProducts() {
        return productMap.values();
    }

    @Override
    public boolean productExists(String id) {
        return productMap.containsKey(id);
    }


    private ConcurrentHashMap<String, Product> createProductList() {
        ConcurrentHashMap<String, Product> productList = new ConcurrentHashMap<>();
        productList.put("1", new Product("1", "T-Shirt", "Pretty nice t-shirt", List.of("se", "us")));
        productList.put("2", new Product("2", "T-Shirt", "Awesome t-shirt", List.of("us"), true));
        productList.put("3", new Product("3", "Pants", "Just some pants", List.of("se")));
        productList.put("4", new Product("4", "T-Shirt", "Basic t-shirt", List.of("de")));
        productList.put("5", new Product("5", "Pants", "A pair of pants", List.of("de"), true));
        return productList;
    }
}
