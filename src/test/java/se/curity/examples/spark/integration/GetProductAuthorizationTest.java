/*
 * Copyright 2023 Curity AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.curity.examples.spark.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.curity.examples.products.Product;
import se.curity.examples.products.ProductServiceMapImpl;
import se.curity.examples.spark.utils.JsonUtil;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetProductAuthorizationTest extends AbstractApiAuthorizationTest {


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
        HttpResponse<String> response = sendAuthenticatedRequest(
                name,
                Map.of("country", country,
                        "subscription_level",subscriptionLevel,
                        "scope", SCOPE),
                applicationUrl("/api/products/" + productId));
        assertEquals(200, response.statusCode(), "Response Code");

        Product product = new ProductServiceMapImpl().getProduct(productId);
        assertEquals(JsonUtil.getJsonObjectWithDescription(product).toString(), response.body());
    }

    @Test
    void returnNotFoundWhenUserIsAuthorizedButProductDoesNotExist() {
        HttpResponse<String> response = sendAuthenticatedRequest(
                "Alice",
                Map.of("country", "se",
                        "subscription_level","trial",
                        "scope", SCOPE),
                applicationUrl("/api/products/-1"));
        assertEquals(404, response.statusCode(), "Response Code");
    }

    @Test
    void denyAccessWhenUserIsNotAuthorizedToAccessExclusiveProduct() {
        HttpResponse<String> response = sendAuthenticatedRequest(
                "Alice",
                Map.of("country", "se",
                        "subscription_level","trial",
                        "scope", SCOPE),
                applicationUrl("/api/products/5"));
        assertEquals(403, response.statusCode(), "Response Code");
    }

    @Test
    void denyAccessWhenSubscriptionLevelIsEmpty() {
        HttpResponse<String> response = sendAuthenticatedRequest(
                "Alice",
                Map.of("country", "se",
                        "subscription_level","",
                        "scope", SCOPE),
                applicationUrl("/api/products/5"));
        assertEquals(403, response.statusCode(), "Response Code");
    }

    @Test
    void denyAccessWhenNoSubscriptionLevel() {
        HttpResponse<String> response = sendAuthenticatedRequest(
                "Alice",
                Map.of("country", "se",
                        "scope", SCOPE),
                applicationUrl("/api/products/5"));
        assertEquals(403, response.statusCode(), "Response Code");
    }

    @Test
    void denyAccessWhenUserLoadsProductFromDifferentCountry() {
        HttpResponse<String> response = sendAuthenticatedRequest(
                "Bob",
                Map.of("country", "us",
                        "subscription_level","trial",
                        "scope", SCOPE),
                applicationUrl("/api/products/5"));
        assertEquals(403, response.statusCode(), "Response Code");
    }
}
