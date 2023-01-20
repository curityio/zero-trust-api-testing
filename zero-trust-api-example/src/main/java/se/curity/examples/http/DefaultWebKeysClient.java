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

import io.curity.oauth.WebKeysClient;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.apache.hc.core5.http.HttpHeaders.ACCEPT;

class DefaultWebKeysClient implements WebKeysClient
{
    private static final Logger _logger = Logger.getLogger(DefaultWebKeysClient.class.getName());
    private final URI _jwksUri;
    private final HttpClient _httpClient;

    DefaultWebKeysClient(URI jwksUri, HttpClient httpClient)
    {
        _jwksUri = jwksUri;
        _httpClient = httpClient;
    }

    @Override
    public String getKeys() throws IOException
    {
        HttpGet get = new HttpGet(_jwksUri);

        get.setHeader(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());


        return _httpClient.execute(get, response -> {

            if (response.getCode() != HttpStatus.SC_OK)
            {
                _logger.severe(() -> "Got error from Jwks server: " + response.getCode());

                throw new IOException("Got error from Jwks server: " + response.getCode());
            }

            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        });
    }
}
