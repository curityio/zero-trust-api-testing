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
package se.curity.examples.spark;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.curity.examples.products.Product;
import spark.Spark;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


@WireMockTest(httpsEnabled = true, httpsPort = 8443)
public class SecureIntegrationTest {

    private static final String ISSUER = "jwtIssuer";
    private static final String AUDIENCE = "someClientId";
    private static final String SCOPE = "read";

    private static MockJwtIssuer mockJwtIssuer;
    private static MockProductServiceImpl mockProductService;

    @BeforeAll
    static void startServer() throws ServletException {
        mockJwtIssuer = new MockJwtIssuer(ISSUER, "key1");
        ServerOptions options = new ServerOptions();
        options.setPort(9090);
        options.setJwksUrl("https://localhost:8443/jwks");
        options.setIssuer(ISSUER);
        options.setAudience(AUDIENCE);
        options.setScope(SCOPE);

        mockProductService = new MockProductServiceImpl();
        SparkServerExample.runLocally(mockProductService, options);
    }

    @BeforeEach
    void publishJwks() throws IOException {
        mockJwtIssuer.publishJwks();
    }

    /**
     * Test that server is up and running. No authentication required.
     */
    @Test
    void unauthenticatedIndexTest() {
        HttpResponse<String> response = sendUnauthenticatedRequest("http://localhost:9090/");
        assertEquals(200, response.statusCode(), "Response Code");
        assertEquals("Welcome!", response.body());
    }

    /**
     * Test that api endpoint requires authentication, i.e. it returns 401 if JWT is missing
     */
    @ParameterizedTest
    @ValueSource(strings = { "/api", "/api/products", "/api/products/99"})
    void requiresAuthentication(String path) {
        HttpResponse<String> response = sendUnauthenticatedRequest("http://localhost:9090" + path);
        assertEquals(401, response.statusCode(), path + " endpoint requires authentication");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api", "/api/products", "/api/products/99"})
    void requiresScopeRead(String path) {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("scope", "someOtherScope"), "http://localhost:9090" + path);
        assertEquals(403, response.statusCode(), "Response Code");
    }

    @ParameterizedTest
    @ValueSource(strings = { "se", "us", "de"})
    void listProductsForCountry(String country) {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", country, "scope", "read"), "http://localhost:9090/api/products");
        assertEquals(200, response.statusCode(), "Response Code");

        Collection<Product> productList = mockProductService.getProductForCountry(country);
        assertEquals(JsonUtil.getJsonArrayFromCollection(productList).toString(), response.body());
    }

    @Test
    void listProductsEmptyCountry() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Bob", Map.of("country", "", "scope", "read"), "http://localhost:9090/api/products");
        assertEquals(200, response.statusCode(), "Response Code");
        assertEquals("[]", response.body());
    }

    @Test
    void listProductsNoCountry() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Bob", Map.of("scope", "read"), "http://localhost:9090/api/products");
        assertEquals(200, response.statusCode(), "Response Code");
        assertEquals("[]", response.body());
    }

    @Test
    void getProductIsAuthorized() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", "se", "subscription_level","trial", "scope", "read"), "http://localhost:9090/api/products/1");
        assertEquals(200, response.statusCode(), "Response Code");

        Product product1 = mockProductService.getProduct("1");

        assertEquals(JsonUtil.getJsonObjectWithDescription(product1).toString(), response.body());
    }

    @Test
    void getUnkownProductIsAuthorized() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", "se", "subscription_level","trial", "scope", "read"), "http://localhost:9090/api/products/-1");
        assertEquals(404, response.statusCode(), "Response Code");
    }

    @Test
    void getProductWrongSubscriptionLevel() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", "se", "subscription_level","trial", "scope", "read"), "http://localhost:9090/api/products/5");
        assertEquals(403, response.statusCode(), "Response Code");
    }

    @Test
    void getUnkownProductEmptySubscriptionLevel() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", "se", "subscription_level","", "scope", "read"), "http://localhost:9090/api/products/5");
        assertEquals(403, response.statusCode(), "Response Code");
    }

    @Test
    void getProductWrongCountry() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Bob", Map.of("country", "us", "subscription_level","trial", "scope", "read"), "http://localhost:9090/api/products/5");
        assertEquals(403, response.statusCode(), "Response Code");
    }

    // TODO: add test for invalid scope

    /**
     * Send an authenticated request to the given url
     * @param subjectName name of authenticated user
     * @param claims claim names and values that should be added to the user's token
     * @param url endpoint to send request to
     * @return response from server as string or null if there was an error.
     */
    private HttpResponse<String> sendAuthenticatedRequest(String subjectName, Map<String, String> claims, String url) {
        String jwt = mockJwtIssuer.getJwt(subjectName, claims, AUDIENCE);
        return sendRequest(url, jwt);
    }

    /**
     * Send an unauthenticated request to the given url
     * @param url endpoint to send request to
     * @return response endpoint as sent by server
     */
    private HttpResponse<String> sendUnauthenticatedRequest(String url) {
        return sendRequest(url, null);
    }

    /**
     * Send a request to the given url and add jwt to authorization header if available
     * @param urlString endpoint to send request to
     * @param jwt optional, token to add to the authorization header
     * @return response from server/endpoint
     */
    private HttpResponse<String> sendRequest(String urlString, @Nullable String jwt) {
        try {
            URI uri = new URI(urlString);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder httpRequestBuilder = HttpRequest
                    .newBuilder()
                    .uri(uri)
                    .header("accept", "application/json")
                    .GET();

            // Add JWT as bearer token if available
            if (jwt != null) {
                httpRequestBuilder.header("Authorization", String.format("Bearer %s", jwt));
            }

            // return response body as string
            return client.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException exception) {
          Assertions.fail(String.format("Cannot send request to %s", urlString));
        }
            return null;
    }

    @AfterAll
    static void stopServer() {
        Spark.awaitStop();
    }
}
