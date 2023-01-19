package se.curity.examples.spark;

import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.curity.examples.http.UnsafeHttpClientSupplier;
import se.curity.examples.products.Product;
import spark.Spark;

import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;


@WireMockTest(httpsEnabled = true, httpsPort = 8443)
public class SparkServerExampleTest {

    private static final String ISSUER = "jwtIssuer";
    private static final String AUDIENCE = "someClientId";
    private static final String SCOPE = "read";

    private static RsaJsonWebKey signingKey;

    private static ProductServiceTestImpl testProductService;

    @BeforeAll
    static void startServer() throws ServletException {
        signingKey = createKeyPair("key1");
        ServerOptions options = new ServerOptions();
        options.setPort(9090);
        options.setJwksUrl("https://localhost:8443/jwks");
        options.setIssuer(ISSUER);
        options.setAudience(AUDIENCE);
        options.setScope(SCOPE);

        testProductService = new ProductServiceTestImpl();
        SparkServerExample.runLocally(testProductService, options);
    }

    @BeforeEach
    void publishJwks() throws IOException {
        String jwks = new JsonWebKeySet(signingKey).toJson();
        stubFor(get("/jwks").willReturn(ok(jwks)));

        // Make sure, jwks is published (disable certificate verification for this purpose)
        Supplier<org.apache.hc.client5.http.classic.HttpClient> unsafeHttpClientSupplier = new UnsafeHttpClientSupplier();
        org.apache.hc.client5.http.classic.HttpClient httpClient = unsafeHttpClientSupplier.get();

        httpClient.execute(new HttpGet("https://localhost:8443/jwks"), response -> {
            assert(response.getCode() == HttpStatus.SC_OK);
            assert(jwks.equals(EntityUtils.toString(response.getEntity())));
            return null;
        });
    }

    /**
     * Test that server is up and running
     * @throws URISyntaxException if index uri is wrong (error in test setup)
     * @throws IOException if client fails to send message
     */
    @Test
    void indexTest() throws URISyntaxException, IOException {
        URI uri = new URI("http://localhost:9090/");
        try (CloseableHttpClient client = HttpClientFactory.createClient()) {

            client.execute(new HttpGet(uri), resp -> {
                assertEquals(200, resp.getCode(), "Response Code");
                assertEquals("Welcome!", EntityUtils.toString(resp.getEntity()), "Index");
                return null;
            });
        }
    }

    /**
     * Test that api endpoint requires authentication, i.e. it returns 401 if JWT is missing
     */
    @Test
    void requiresAuthentication() {
        HttpResponse<String> response = sendUnauthenticatedRequest("http://localhost:9090/api");
        assertEquals(401, response.statusCode(), "/api endpoint requires authentication");

        response = sendUnauthenticatedRequest("http://localhost:9090/api/something");
        assertEquals(401, response.statusCode(), "Subpath in /api require authentication");
    }


    @Test
    void listProductsSE() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", "se", "scope", "read"), "http://localhost:9090/api/products");
        assertEquals(200, response.statusCode(), "Response Code");

        Collection<Product> productList = testProductService.getProductForCountry("se");
        JsonArrayBuilder jsonProductListBuilder = Json.createArrayBuilder();
        productList.forEach(product -> {
            jsonProductListBuilder.add(getJsonObject(product, false));
        });
        assertEquals(jsonProductListBuilder.build().toString(), response.body());
    }

    @Test
    void listProductsUS() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Bob", Map.of("country", "us", "scope", "read"), "http://localhost:9090/api/products");
        assertEquals(200, response.statusCode(), "Response Code");

        Collection<Product> productList = testProductService.getProductForCountry("us");
        JsonArrayBuilder jsonProductListBuilder = Json.createArrayBuilder();
        productList.forEach(product -> {
            jsonProductListBuilder.add(getJsonObject(product, false));
        });
        assertEquals(jsonProductListBuilder.build().toString(), response.body());
    }

    @Test
    void getProduct() {
        //TODO: use WireMockRuntimeInfo runtimeInfo to retrieve port
        HttpResponse<String> response = sendAuthenticatedRequest("Alice", Map.of("country", "se", "subscription_level","trial", "scope", "read"), "http://localhost:9090/api/products/1");
        assertEquals(200, response.statusCode(), "Response Code");

        Product product1 = testProductService.getProduct("1");

        assertEquals(getJsonObject(product1, true).toString(), response.body());
    }

    private static JsonObject getJsonObject(Product product, boolean includeDescription) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder .add("id", product.getId())
                .add("name", product.getName())
                .add("isExclusive", product.IsExclusive());

        if (includeDescription) {
            builder.add("description", product.getDetails());
        }

        return builder.build();
    }

    // TODO: add negative tests for product list (empty list if no country) and product id (invalid id => not found, invalid subscription => access denied)

    // TODO: add test for invalid scope

    /**
     * Send an authenticated request to the given url
     * @param subjectName name of authenticated user
     * @param claims claim names and values that should be added to the user's token
     * @param url endpoint to send request to
     * @return response from server as string or null if there was an error.
     */
    private HttpResponse<String> sendAuthenticatedRequest(String subjectName, Map<String, String> claims, String url) {
        String jwt = getJwt(subjectName, claims);
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

    /**
     * Create a default JWT
     * @param subjectName name of the subject (value of 'sub' claim)
     * @param claims other claim names and values to indluce in the JWT
     * @return jwt for the given subject that includes the specified claims and default values for issuer and audience
     */
    private String getJwt(String subjectName, Map<String, String> claims) {
        return getJwt(subjectName, claims, Optional.empty(), Optional.empty());
    }

    /**
     * Create a JWT using the default signing key
     * @param subjectName name of the subject (value of 'sub' claim)
     * @param claims other claims names and values to include in the JWT
     * @param issuer name of the issuer (value of 'iss' claim)
     * @param audience name of the audience (value of 'aud' claim)
     * @return String representation of a JWT as defined in RFC 7915
     */
    private String getJwt(String subjectName, Map<String, String> claims, Optional<String> issuer, Optional<String> audience) {
        // Create the Claims, which will be the content of the JWT
        JwtClaims jwtClaims = new JwtClaims();

        if (claims != null) {
            claims.forEach(jwtClaims::setStringClaim);
        }

        // Add common claims
        jwtClaims.setSubject(subjectName);
        jwtClaims.setIssuer(issuer.orElse(ISSUER));
        jwtClaims.setAudience(audience.orElse(AUDIENCE));
        jwtClaims.setExpirationTimeMinutesInTheFuture(10);
        jwtClaims.setGeneratedJwtId();
        jwtClaims.setIssuedAtToNow();
        jwtClaims.setNotBeforeMinutesInThePast(2);

        // Sign claims with the default signing key and RS256
        try {
            JsonWebSignature jws = new JsonWebSignature();
            jws.setPayload(jwtClaims.toJson());

            jws.setKey(signingKey.getPrivateKey());
            jws.setKeyIdHeaderValue(signingKey.getKeyId());
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

            return jws.getCompactSerialization();
        } catch (JoseException exception) {
            Assertions.fail(String.format("Error when creating JWT: %s", exception.getMessage()));
            return "";
        }
    }

    /**
     * Create new RSA key with the given kid for signing.
     * @param kid key id of the json web key (JWK)
     * @return the jwk representation of the RSA key
     */
    private static RsaJsonWebKey createKeyPair(String kid) {
        try {
            RsaJsonWebKey signingKey = RsaJwkGenerator.generateJwk(2048);
            signingKey.setKeyId(kid);
            return signingKey;
        } catch (JoseException joseException) {
            Assertions.fail(String.format("Error when creating RSA key: %s", joseException.getMessage()));
            return null;
        }
    }


    @AfterAll
    static void stopServer() {
        Spark.stop();
    }
}
