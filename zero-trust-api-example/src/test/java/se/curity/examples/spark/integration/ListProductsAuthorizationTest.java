package se.curity.examples.spark.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
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
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", country, "scope", "read"), serverUrl("/api/products"));
        assertEquals(200, response.statusCode(), "Response Code");

        Collection<Product> productList = mockProductService.getProductForCountry(country);
        assertEquals(JsonUtil.getJsonArrayFromCollection(productList).toString(), response.body());
    }

    @Test
    void returnEmptyProductListWhenCountryIsEmpty() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Bob", Map.of("country", "", "scope", "read"), serverUrl("/api/products"));
        assertEquals(200, response.statusCode(), "Response Code");
        assertEquals("[]", response.body());
    }

    @Test
    void returnEmptyProductListWhenCountryIsMissing() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Bob", Map.of("scope", "read"), serverUrl("/api/products"));
        assertEquals(200, response.statusCode(), "Response Code");
        assertEquals("[]", response.body());
    }
}
