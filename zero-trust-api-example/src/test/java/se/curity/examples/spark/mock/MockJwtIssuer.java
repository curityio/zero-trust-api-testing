package se.curity.examples.spark.mock;

import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

public class MockJwtIssuer {


    private final RsaJsonWebKey SIGNING_KEY;
    private final String DEFAULT_ISSUER;

    public MockJwtIssuer(String issuer, String kid) {
        SIGNING_KEY = createKeyPair(kid);
        DEFAULT_ISSUER = issuer;
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

    public String getJwt(String subjectName, Map<String, String> claims, String audience) {
        return getJwt(subjectName, claims, DEFAULT_ISSUER, audience);
    }

    /**
     * Create a JWT using the default signing key
     * @param subjectName name of the subject (value of 'sub' claim)
     * @param claims other claims names and values to include in the JWT
     * @param issuer name of the issuer (value of 'iss' claim)
     * @param audience name of the audience (value of 'aud' claim)
     * @return String representation of a JWT as defined in RFC 7915
     */
    public String getJwt(String subjectName, Map<String, String> claims, String issuer, String audience) {
        // Create the Claims, which will be the content of the JWT
        JwtClaims jwtClaims = new JwtClaims();

        if (claims != null) {
            claims.forEach(jwtClaims::setStringClaim);
        }

        // Add common claims
        jwtClaims.setSubject(subjectName);
        jwtClaims.setIssuer(Objects.requireNonNull(issuer));
        jwtClaims.setAudience(Objects.requireNonNull(audience));
        jwtClaims.setExpirationTimeMinutesInTheFuture(10);
        jwtClaims.setGeneratedJwtId();
        jwtClaims.setIssuedAtToNow();
        jwtClaims.setNotBeforeMinutesInThePast(2);

        // Sign claims with the default signing key and RS256
        try {
            JsonWebSignature jws = new JsonWebSignature();
            jws.setPayload(jwtClaims.toJson());

            jws.setKey(SIGNING_KEY.getPrivateKey());
            jws.setKeyIdHeaderValue(SIGNING_KEY.getKeyId());
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

            return jws.getCompactSerialization();
        } catch (JoseException exception) {
            Assertions.fail(String.format("Error when creating JWT: %s", exception.getMessage()));
            return "";
        }
    }

    public void publishJwks() {
        String jwks = new JsonWebKeySet(SIGNING_KEY).toJson();
        stubFor(get("/jwks").willReturn(ok(jwks)));
    }
}
