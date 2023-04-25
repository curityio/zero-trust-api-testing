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

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Options that can be passed on to the @SparkServerExample
 */
public class ServerOptions {

    /**
     * --port: the port number that the server will run at
     */
    private int port;

    /**
     * --jwksurl: the URL where the JSON Web Key Set can be found. This key set contains the key to verify the signature of JWTs.
     */
    private URL jwksUrl;

    /**
     * --issuer: the expected value of the 'iss' claim in the JWT. This value is used during JWT validation.
     */
    private String issuer;

    /**
     * --audience: the expected value of the 'aud' claim in the JWT. This value is used during JWT validation.
     */
    private String audience;

    /**
     * --scope: the expected value of the 'scope' claim in the JWT. This value is used during JWT validation.
     */
    private String scope;

    /**
     * Get the configured port number
     * @return port number as int
     */
    public int getPort() { return port; }

    public void setPort(int port) { if(port > 0) this.port = port;}

    /**
     * Get the port of the JWKS URL
     * @return the string representation of the port in the JWKS URL.
     */
    public String getJwksPort() {
        if (jwksUrl.getPort() == -1) {
            // No custom port defined. Return default.
            return "" + jwksUrl.getDefaultPort();
        } else {
            return "" + jwksUrl.getPort();
        }
    }

    /**
     * Get the host name of the JWKS URL
     * @return the host name of the JWKS URL
     */
    public String getJwksHost() { return jwksUrl.getHost(); }

    /**
     * Get the path part of the JWKS URL
     * @return the path part of this URL, or an empty string if one does not exist
     */
    public String getJwksPath() { return jwksUrl.getPath(); }

    public void setJwksUrl(String url) {
        try {
            this.jwksUrl = new URL(url);
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException(String.format("Invalid value for JWKS URL: %s", exception.getMessage()), exception);
        }
    }

    /**
     * Get the configured expected value of the JWT issuer ('iss' claim)
     * @return the expected value of the JWT issuer
     */
    public String getIssuer() { return issuer; }

    public void setIssuer(String issuer) { this.issuer = issuer; }

    /**
     * Get the configured expected audience of the JWT ('aud' claim)
     * @return the expected value of the JWT audience
     */
    public String getAudience() { return audience; }

    public void setAudience(String audience) { this.audience = audience; }
    /**
     * Get the configured required scopes for a JWT to be valid
     * @return the configured scope value that the server will accept.
     */
    public String getScope() { return scope; }

    public void setScope(String scope) { this.scope = scope; }

    /**
     * Create default options:
     * port: 9090 <br/>
     * issuer: https://localhost:8443/oauth/v2/oauth-anonymous <br/>
     * audience: www <br/>
     * scope: read <br/>
     * jwksurl: https://localhost:8443/oauth/v2/oauth-anonymous/jwks
     */
    public ServerOptions() {
        this.port = 9090;
        this.issuer = "https://localhost:8443/oauth/v2/oauth-anonymous";
        this.audience = "api.example.com";
        this.scope = "products";
        try {
            this.jwksUrl = new URL("https", "localhost", 8443, "/oauth/v2/oauth-anonymous/jwks");
        } catch (MalformedURLException exception) {
            // not going to happen because values are hardcoded.
        }
    }

    /**
     * Parse options from command line arguments.
     * --port <port number of this application>
     * --issuer <Expected value of iss claim in JWT
     * --jwksurl <URL to JWKS>
     * --audience <Expected value aud claim in JWT>
     * --scope <Expected scopes in JWT>);
     * @param args an optional list of arguments. If empty or null, default values will be used.
     */
    public ServerOptions(@Nullable String[] args) {
        // set default values
        this();
        if (args != null && args.length > 0) {

            if (args.length % 2 != 0) {
                throw new IllegalArgumentException("Invalid number of options. Use [--port <port number of this application>] [--issuer <Expected value of iss claim in JWT>] [--jwksurl <URL to JWKS>] [--audience <Expected aud claim in jwt> [--scope <Expected scopes in jwt>]");
            }

            for (int i = 0; i< args.length-1; i = i+2) {
                String argumentName = args[i].toLowerCase();
                String argumentValue = args[i+1];

                if (argumentName.startsWith("--") && argumentValue == null || argumentValue.isBlank() || argumentValue.startsWith("-")) {
                    throw new IllegalArgumentException(String.format("Invalid value for %s. Use [--port <port number of this application>] [--issuer <Expected value of iss claim in JWT>] [--jwksurl <URL to JWKS>] [--audience <Expected aud claim in jwt> [--scope <Expected scopes in jwt>]", argumentName));
                }

                switch (argumentName) {
                    case "--jwksurl" -> {
                        try {
                            this.jwksUrl = new URL(argumentValue);
                        } catch (MalformedURLException exception) {
                            throw new IllegalArgumentException(String.format("Invalid value for JWKS URL: %s", exception.getMessage()));
                        }
                    }
                    case "--issuer" -> this.issuer = argumentValue;
                    case "--port" -> {
                        try {
                            this.port = Integer.parseInt(argumentValue);
                        } catch (NumberFormatException exception) {
                            throw new IllegalArgumentException(String.format("Invalid value for port: %s", exception.getMessage()));
                        }
                    }
                    case "--audience" -> this.audience = argumentValue;
                    case "--scope" -> this.scope = argumentValue;
                    default -> throw new IllegalArgumentException(String.format("Unknown argument %s. Use [--port <port number of this application>] [--issuer <Expected value of iss claim in JWT>] [--jwksurl <URL to JWKS>] [--audience <Expected aud claim in jwt> [--scope <Expected scopes in jwt>]", argumentName));
                }
            }
        }
    }

}
