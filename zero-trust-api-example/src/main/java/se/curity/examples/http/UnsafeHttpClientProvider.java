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
package se.curity.examples.http;

import io.curity.oauth.IntrospectionClient;
import io.curity.oauth.OAuthFilter;
import io.curity.oauth.WebKeysClient;
import org.apache.hc.client5.http.classic.HttpClient;

import javax.servlet.UnavailableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UnsafeHttpClientProvider extends io.curity.oauth.HttpClientProvider {
    private static final Logger _logger = Logger.getLogger(UnsafeHttpClientProvider.class.getName());

    private interface InitParams
    {
        String OAUTH_HOST = "oauthHost";
        String OAUTH_PORT = "oauthPort";
        String JSON_WEB_KEYS_PATH = "jsonWebKeysPath";
        String INTROSPECTION_PATH = "introspectionPath";
        String CLIENT_ID = "clientId";
        String CLIENT_SECRET = "clientSecret";
    }

    @Override
    public IntrospectionClient createIntrospectionClient(Map<String, ?> config) throws UnavailableException {

        String oauthHost = getInitParamValue(InitParams.OAUTH_HOST, config);
        int oauthPort = getInitParamValue(InitParams.OAUTH_PORT, config, Integer::parseInt);
        String introspectionPath = getInitParamValue(InitParams.INTROSPECTION_PATH, config);
        String clientId = getInitParamValue(InitParams.CLIENT_ID, config);
        String clientSecret = getInitParamValue(InitParams.CLIENT_SECRET, config);

        URI introspectionUri;
        try
        {
            introspectionUri = new URI("https", null, oauthHost, oauthPort, introspectionPath, null, null);
        }
        catch (URISyntaxException e)
        {
            _logger.log(Level.SEVERE, "Invalid parameters", e);

            throw new UnavailableException("Service is unavailable");
        }

        Supplier<HttpClient> unsafeHttpClientSupplier = new UnsafeHttpClientSupplier();
        HttpClient httpClient = unsafeHttpClientSupplier.get();

        return new DefaultIntrospectClient(introspectionUri, clientId, clientSecret, httpClient);
    }

    @Override
    public WebKeysClient createWebKeysClient(Map<String, ?> config) throws UnavailableException {
        URI webKeysUri;

        try
        {
            int oauthPort = getInitParamValue(InitParams.OAUTH_PORT, config, Integer::parseInt);
            String webKeysPath = getInitParamValue(InitParams.JSON_WEB_KEYS_PATH, config);
            String oauthHost = getInitParamValue(InitParams.OAUTH_HOST, config);

            webKeysUri = new URI("https", null, oauthHost, oauthPort, webKeysPath, null, null);
        }
        catch (URISyntaxException e)
        {
            _logger.log(Level.SEVERE, "Invalid parameters", e);

            throw new UnavailableException("Service is unavailable");
        }

        Supplier<HttpClient> unsafeHttpClientSupplier = new UnsafeHttpClientSupplier();
        HttpClient httpClient = unsafeHttpClientSupplier.get();

        return new DefaultWebKeysClient(webKeysUri, httpClient);
    }

    static String getInitParamValue(String name, Map<String, ?> initParams) throws UnavailableException
    {
        Optional<String> value = getSingleValue(name, initParams);

        if (value.isPresent())
        {
            return value.get();
        }
        else
        {
            throw new UnavailableException(missingInitParamMessage(name));
        }
    }

    static <T> T getInitParamValue(String name, Map<String, ?> initParams,
                                   Function<String, T> converter) throws UnavailableException
    {
        return converter.apply(getInitParamValue(name, initParams));
    }

    private static Optional<String> getSingleValue(String name, Map<String, ?> initParams) {
        return Optional.ofNullable(initParams.get(name)).map(Object::toString);
    }

    private static String missingInitParamMessage(String paramName)
    {
        return String.format("%s - missing required initParam [%s]",
                OAuthFilter.class.getName(),
                paramName);
    }
}
