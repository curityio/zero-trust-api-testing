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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import se.curity.examples.spark.mock.MockJwtIssuer;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;

public abstract class AbstractApiAuthorizationTest {

    static final String ISSUER = "http://localhost:8443/oauth/v2/oauth-anonymous";
    static final String AUDIENCE = "api.example.com";
    static final String JWKS_PATH = "/oauth/v2/oauth-anonymous/jwks";
    static final String SCOPE = "products";
    static final int PORT = 9090;

    private static boolean started = false;

    /**
     * Creates JWTs for the given issuer and a generated key ID
     */
    static MockJwtIssuer mockJwtIssuer = new MockJwtIssuer(ISSUER, UUID.randomUUID().toString());
    /**
     * Used to mock JWKS endpoint for mocked JWT issuer
     */
    static WireMockServer mockAuthorizationServer;

    @BeforeAll
    public static void startMockAuthorizationServer() throws ServletException {

        if (started) {
            return;
        }

        started = true;
        var options = new WireMockConfiguration().port(8443);
        mockAuthorizationServer = new WireMockServer(options);
        mockAuthorizationServer.start();

        String jwksUrl = mockAuthorizationServer.baseUrl() + JWKS_PATH;
        Logger.getLogger(AbstractApiAuthorizationTest.class.getName()).info("Mocked JWKS URL on " + jwksUrl);
        mockAuthorizationServer.stubFor(get(JWKS_PATH)
                .willReturn(
                        ok(mockJwtIssuer.getJwks())
                )
        );
    }

    /**
     * Send an authenticated request to the given url
     * @param subjectName name of authenticated user
     * @param claims claim names and values that should be added to the user's token
     * @param url endpoint to send request to
     * @return response from server as string or null if there was an error.
     */
    HttpResponse<String> sendAuthenticatedRequest(String subjectName, Map<String, String> claims, String url) {
        String jwt = mockJwtIssuer.getJwt(subjectName, claims, AUDIENCE);
        return sendRequest(url, jwt);
    }

    /**
     * Send an unauthenticated request to the given url
     * @param url endpoint to send request to
     * @return response as received from server
     */
    HttpResponse<String> sendUnauthenticatedRequest(String url) {
        return sendRequest(url, null);
    }

    /**
     * Send a request to the given url and add JWT to authorization header if available
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

    /**
     * Get the full URL of the given path for the API
     * @param path relative path/file part of URL
     * @return URL including protocol, host, port and path
     */
    String applicationUrl(String path) {
        try {
            return new URL("http", "localhost", PORT, path).toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
