package se.curity.examples.spark;

import org.junit.jupiter.api.Test;
import se.curity.examples.products.Product;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class ListProductsRequestHandlerTest {

    static ProductServiceTestImpl testProductService = new ProductServiceTestImpl();
    static ListProductsRequestHandler listProductsRequestHandler = new ListProductsRequestHandler(testProductService);

    @Test
    void emptyListIfCountryIsMissing() {
        assertTrue(listProductsRequestHandler.getProducts(null).isEmpty(),"Empty product list when country is missing.");
        assertTrue(listProductsRequestHandler.getProducts("").isEmpty(), "Empty product list when country is empty.");
    }

    @Test
    void filterProductListOnCountry() {
        JsonArray filteredForCountry = toJsonArray(testProductService.getProductForCountry("se"));
        assertEquals(filteredForCountry, listProductsRequestHandler.getProducts("se"));
    }

    private JsonArray toJsonArray(Collection<Product> productList) {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        productList.forEach(product -> jsonArrayBuilder.add(toJsonObject(product)));
        return jsonArrayBuilder.build();
    }

    private JsonObject toJsonObject(Product product) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                .add("id", product.getId())
                .add("name", product.getName())
                .add("isExclusive", product.IsExclusive());

        return objectBuilder.build();
    }

    //TODO: add tests for countries
    //TODO: add tests for specific products
}