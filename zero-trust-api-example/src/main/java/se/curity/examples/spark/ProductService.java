package se.curity.examples.spark;

import se.curity.examples.products.Product;
import spark.Request;

import java.util.Collection;

/**
 * These are the service offers
 */
public interface ProductService {
    /**
     * Get a product by an id
     * @param id identifier of the product
     * @return
     */
    Product getProduct(String id);

    /**
     * Get a list of products offered by the service
     * @return
     */
    Collection<Product> getProducts();

    /**
     * Check if the given product exists.
     * @param id identifier of the product
     * @return
     */
    boolean productExists(String id);
}
