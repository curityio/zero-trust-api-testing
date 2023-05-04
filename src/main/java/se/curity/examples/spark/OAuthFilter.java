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

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Arrays;

import static spark.Spark.halt;

/**
 * An OAuth filter to do JWT validation
 */
public class OAuthFilter implements Filter {

    public static final String CLAIMS_PRINCIPAL = "CLAIMS_PRINCIPAL";
    private static final Logger _logger = LoggerFactory.getLogger(OAuthFilter.class);
    private final ServerOptions _options;
    private final HttpsJwksVerificationKeyResolver _httpsJwksKeyResolver;

    public OAuthFilter(ServerOptions options) {
        _options = options;
        _httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(new HttpsJwks(options.getJwksUrl()));
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        var httpRequest = (HttpServletRequest) request;
        var httpResponse = (HttpServletResponse) response;

        try {

            var jwt = this.getBearerToken(httpRequest);
            if (jwt.isEmpty()) {
                _logger.info("No access token was received in the authorization header");
                this.unauthorizedResponse(httpResponse);
                return;
            }

            var jwtConsumer = new JwtConsumerBuilder()
                    .setVerificationKeyResolver(_httpsJwksKeyResolver)
                    .setJwsAlgorithmConstraints(
                            AlgorithmConstraints.ConstraintType.PERMIT,
                            AlgorithmIdentifiers.RSA_USING_SHA256
                    )
                    .setExpectedIssuer(_options.getIssuer())
                    .setExpectedAudience(_options.getAudience())
                    .build();

            var jwtClaims = jwtConsumer.processToClaims(jwt);

            var scopeString = jwtClaims.getStringClaimValue("scope");
            var scopes = scopeString.split(" ");
            var foundScope = Arrays.stream(scopes).filter(s -> s.contains(_options.getScope())).findFirst();
            if (foundScope.isEmpty()) {
                _logger.info("The JWT access token has an invalid scope");
                this.forbiddenResponse(httpResponse);
                return;
            }

            _logger.debug("The request passed JWT validation");
            request.setAttribute(CLAIMS_PRINCIPAL, jwtClaims);

            if (filterChain != null) {
                filterChain.doFilter(request, response);
            }

        } catch (InvalidJwtException ex) {

            _logger.info("JWT validation failed");
            for (var item : ex.getErrorDetails()) {
                String info =String.format("%s : %s", item.getErrorCode(), item.getErrorMessage());
                _logger.debug(info);
            }

            this.unauthorizedResponse(httpResponse);

        } catch (MalformedClaimException ex) {

            _logger.info("The scope could not be found in the JWT access token");
            this.forbiddenResponse(httpResponse);
        }
    }

    @Override
    public void destroy() {
    }

    private String getBearerToken(HttpServletRequest httpRequest) {

        var header = httpRequest.getHeader("Authorization");
        if (header != null) {
            var parts = header.split(" ");
            if (parts.length == 2) {
                if (parts[0].equalsIgnoreCase("bearer")) {
                    return parts[1];
                }
            }
        }

        return "";
    }

    private void unauthorizedResponse(HttpServletResponse httpResponse) {

        httpResponse.setHeader(
                "www-authenticate",
                "Bearer, error=invalid_token, error_description=Access token is missing, invalid or expired");
        halt(401);
    }

    private void forbiddenResponse(HttpServletResponse httpResponse) {
        halt(403);
    }
}
