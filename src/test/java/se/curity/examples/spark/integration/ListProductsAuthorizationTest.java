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
import org.junit.jupiter.params.provider.ValueSource;
import se.curity.examples.products.Product;
import se.curity.examples.products.ProductServiceMapImpl;
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

        Collection<Product> productList = new ProductServiceMapImpl().getProductsForCountry(country);
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
