package se.curity.examples.spark.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.curity.examples.products.Product;
import se.curity.examples.spark.utils.JsonUtil;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetProductIsAuthorizedTest extends AbstractApiAuthorizationTest {

    private static Stream<Arguments> provideInputForAuthorizedRequests() {
        return Stream.of(
                Arguments.of("Alice", "se", "trial", "1"),
                Arguments.of("Alice", "se", "trial", "3"),
                Arguments.of("Alice", "se", "premium", "5"),
                Arguments.of("Bob", "us", "trial", "1"),
                Arguments.of("Bob", "us", "premium", "2"),
                Arguments.of("Clara", "de", "trial", "4"),
                Arguments.of("Clara", "de", "premium", "4")
        );
    }

    /**
     * Test that users can access products they are authorized to access
     * @param name name of the user
     * @param country country the user is located in
     * @param subscriptionLevel level of subscription the user signed up for
     * @param productId id of the product the user aims to access
     */
    @ParameterizedTest
    @MethodSource("provideInputForAuthorizedRequests")
    void returnJsonObjectForProductWhenUserIsAuthorized(String name, String country, String subscriptionLevel, String productId) {
        HttpResponse<String> response = sendAuthenticatedRequest(name, Map.of("country", country, "subscription_level",subscriptionLevel, "scope", "read"), serverUrl("/api/products/" + productId));
        assertEquals(200, response.statusCode(), "Response Code");

        Product product = mockProductService.getProduct(productId);

        assertEquals(JsonUtil.getJsonObjectWithDescription(product).toString(), response.body());
    }

    @Test
    void returnNotFoundWhenUserIsAuthorizedButProductDoesNotExist() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", "se", "subscription_level","trial", "scope", "read"), serverUrl("/api/products/-1"));
        assertEquals(404, response.statusCode(), "Response Code");
    }

    @Test
    void denyAccessWhenUserIsNotAuthorizedToAccessExclusiveProduct() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", "se", "subscription_level","trial", "scope", "read"), serverUrl("/api/products/5"));
        assertEquals(403, response.statusCode(), "Response Code");
    }

    @Test
    void denyAccessWhenSubscriptionLevelIsEmpty() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", "se", "subscription_level","", "scope", "read"), serverUrl("/api/products/5"));
        assertEquals(403, response.statusCode(), "Response Code");
    }

    @Test
    void denyAccessWhenNoSubscriptionLevel() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", "se", "subscription_level","", "scope", "read"), serverUrl("/api/products/5"));
        assertEquals(403, response.statusCode(), "Response Code");
    }
    @Test
    void denyAccessWhenUserLoadsProductFromDifferentCountry() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Bob", Map.of("country", "us", "subscription_level","trial", "scope", "read"), serverUrl("/api/products/5"));
        assertEquals(403, response.statusCode(), "Response Code");
    }

}
