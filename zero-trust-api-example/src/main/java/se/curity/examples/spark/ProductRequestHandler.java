package se.curity.examples.spark;

import io.curity.oauth.AuthenticatedUser;
import io.curity.oauth.OAuthFilter;
import se.curity.examples.exceptions.AuthorizationException;
import se.curity.examples.exceptions.NotFoundException;
import se.curity.examples.products.Product;
import spark.Request;
import spark.Route;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static spark.Spark.halt;

public abstract class ProductRequestHandler implements Route {

    private ProductService productService;

    static final String CLAIM_NAME_COUNTRY = "country";
    static final String CLAIM_NAME_SUBSCRIPTION_LEVEL = "subscription_level";

    public ProductRequestHandler(ProductService productService) {
        this.productService = productService;
    }

    /**
     * List the products that are accessible from the current request, aka user.
     *
     * @param countryCode  country code to filter products for
     * @return A filtered list of products that are available for the user
     */
    protected Collection<Product> filterProducts(String countryCode) {
        if (!countryCode.isBlank()) {
            return productService.getProducts().stream()
                    .filter((product) -> {
                        Collection<String> authorizedCountriesForProduct = product.getAuthorizedCountries();
                        return switch (countryCode) {
                            case "se" -> authorizedCountriesForProduct.contains("se");
                            case "de" -> authorizedCountriesForProduct.contains("de");
                            case "gb" -> authorizedCountriesForProduct.contains("gb");
                            case "us" -> authorizedCountriesForProduct.contains("us");
                            default -> false;
                        };
                    })
                    .toList();
        } else {
            Map<String, Product> emptyResult = Collections.emptyMap();
            return emptyResult.values();
        }
    }

    protected Product getProduct(String countryCode, String subscriptionLevel, String productId) throws AuthorizationException, NotFoundException {

        // Only users with a subscription may view product details
        if (!subscriptionLevel.isBlank()) {

            // If product exists, perform authorization
            if (productService.productExists(productId)) {
                // Get the list of products visible for the user
                Collection<Product> filteredProductList = filterProducts(countryCode);
                Product product = productService.getProduct(productId);

                if (filteredProductList.contains(product)) {
                    // Product is visible for the user but exclusive
                    if (product.IsExclusive()) {
                        if ("premium".equals(subscriptionLevel)) {
                            // Premium users can view details of exclusive products
                            return product;
                        } else {
                            // Users of any other subscriptions are not allowed to view details of exclusive products
                            throw new AuthorizationException();
                        }
                    } else {
                        // Non-exclusive products are available for any subscription
                        return product;
                    }
                } else {
                    // Product is not available for the user (e.g. wrong country)
                    throw new AuthorizationException();
                }
            } else {
                // Product does not exist
                throw new NotFoundException();
            }
        } else {
            // No active subscription
            throw  new AuthorizationException("Missing subscription");
        }
    }

    protected static JsonObject getJsonObject(Product product, boolean includeDescription) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                .add("id", product.getId())
                .add("name", product.getName())
                .add("isExclusive", product.IsExclusive());

        if (includeDescription) {
            objectBuilder.add("description", product.getDetails());
        }

        return objectBuilder.build();
    }
}
