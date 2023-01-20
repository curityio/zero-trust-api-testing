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

import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ApiAuthorizationTest extends AbstractApiAuthorizationTest {

    /**
     * Test that server is up and running. No authentication required.
     */
    @Test
    void unauthenticatedIndexTest() {
        HttpResponse<String> response = sendUnauthenticatedRequest(serverUrl("/"));
        assertEquals(200, response.statusCode(), "Response Code");
        assertEquals("Welcome!", response.body());
    }

    /**
     * Test that api endpoint requires authentication, i.e. it returns 401 if JWT is missing
     */
    @ParameterizedTest
    @ValueSource(strings = { "/api", "/api/products", "/api/products/99"})
    void returnsUnauthorizedIfJwtIsMissing(String path) {
        HttpResponse<String> response = sendUnauthenticatedRequest(serverUrl(path));
        assertEquals(401, response.statusCode(), path + " endpoint requires authentication");
    }

    /**
     * Test that api endpoint is not accessible with invalid scope
     * @param path the path of the URL to test
     */
    @ParameterizedTest
    @ValueSource(strings = { "/api", "/api/products", "/api/products/99"})
    void returnsUnauthorizedWhenScopeIsInvalid(String path) {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("scope", "someOtherScope"), serverUrl("/api"));
        assertEquals(403, response.statusCode(), "Response Code");
    }

    /**
     * Test that api endpoint is not found when JWT includes correct scope (instead of access denied)
     */
    @Test
    void returnsNotFoundWhenScopeIsRead() {
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("scope", "read"), serverUrl("/api") );
        assertEquals(404, response.statusCode(), "Response Code");
    }
}
