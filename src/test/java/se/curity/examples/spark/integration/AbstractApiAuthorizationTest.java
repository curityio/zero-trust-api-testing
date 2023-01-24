package se.curity.examples.spark.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import se.curity.examples.spark.ServerOptions;
import se.curity.examples.spark.SparkServerExample;
import se.curity.examples.spark.mock.MockJwtIssuer;
import se.curity.examples.spark.mock.MockProductServiceImpl;
import spark.Spark;

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
import java.util.logging.Logger;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public abstract class AbstractApiAuthorizationTest {

    static final String ISSUER = "jwtIssuer";
    static final String AUDIENCE = "someClientId";
    static final String SCOPE = "read";
    static final int PORT = 9090;

    static MockProductServiceImpl mockProductService;
    static boolean applicationStarted;

    /**
     * Creates JWTs for the given issuer and key
     */
    static MockJwtIssuer mockJwtIssuer = new MockJwtIssuer(ISSUER, "key1");
    /**
     * Used to mock JWKS endpoint for mocked JWT issuer
     */
    @RegisterExtension
    static WireMockExtension wm1 = WireMockExtension.newInstance()
            .options(wireMockConfig().httpDisabled(true).httpsPort(8443))
            .build();

    @BeforeAll
    static synchronized void startApplication() throws ServletException {
        String jwksUrl = wm1.baseUrl() + "/jwks";
        Logger.getLogger(AbstractApiAuthorizationTest.class.getName()).info("Mocked JWKS URL on " + jwksUrl);

        ServerOptions options = new ServerOptions();
        options.setPort(PORT);
        options.setJwksUrl(jwksUrl);
        options.setIssuer(ISSUER);
        options.setAudience(AUDIENCE);
        options.setScope(SCOPE);

        if (!applicationStarted) {
            mockProductService = new MockProductServiceImpl();;
            SparkServerExample.runLocally(mockProductService, options);
            applicationStarted = true;
        }
    }

    @BeforeEach
    void publishJwks() {
        wm1.stubFor(get("/jwks")
                .willReturn(
                        ok(mockJwtIssuer.getJwks())
                )
        );
    }

    @AfterAll
    static synchronized void stopApplication() {
        Spark.awaitStop();
    }


    String applicationUrl(String path) {
        try {
            return new URL("http", "localhost", PORT, path).toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
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
}
