package se.curity.examples.spark.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.curity.examples.products.Product;
import se.curity.examples.spark.utils.JsonUtil;

import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListProductsAuthorizationTest extends AbstractApiAuthorizationTest  {

    @ParameterizedTest
    @ValueSource(strings = { "se", "us", "de"})
    void returnProductListForCountry(String country) {
        HttpResponse<String> response = sendAuthenticatedRequest(
                "Alice",
                Map.of("country", country,
                        "scope", SCOPE),
                applicationUrl("/api/products"));
        assertEquals(200, response.statusCode(), "Response Code");

        Collection<Product> productList = mockProductService.getProductForCountry(country);
        assertEquals(JsonUtil.getJsonArrayFromCollection(productList).toString(), response.body());
    }

    @Test
    void returnEmptyProductListWhenCountryIsEmpty() {
        HttpResponse<String> response = sendAuthenticatedRequest(
                "Bob", Map.of(
                        "country", "",
                        "scope", SCOPE),
                applicationUrl("/api/products"));
        assertEquals(200, response.statusCode(), "Response Code");
        assertEquals("[]", response.body());
    }

    @Test
    void returnEmptyProductListWhenCountryIsMissing() {
        HttpResponse<String> response = sendAuthenticatedRequest(
                "Bob",
                Map.of("scope", SCOPE),
                applicationUrl("/api/products"));
        assertEquals(200, response.statusCode(), "Response Code");
        assertEquals("[]", response.body());
    }
}
