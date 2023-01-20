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
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.apache.hc.core5.http.HttpHeaders.ACCEPT;

class DefaultIntrospectClient implements IntrospectionClient
{
    private static final Logger _logger = Logger.getLogger(DefaultIntrospectClient.class.getName());

    private final HttpClient _httpClient;
    private final URI _introspectionUri;
    private final String _clientId;
    private final String _clientSecret;

    DefaultIntrospectClient(URI introspectionUri, String clientId, String clientSecret, HttpClient httpClient)
    {
        _introspectionUri = introspectionUri;
        _clientId = clientId;
        _clientSecret = clientSecret;
        _httpClient = httpClient;
    }

    @Override
    public String introspect(String token) throws IOException
    {
        HttpPost post = new HttpPost(_introspectionUri);

        post.setHeader(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

        List<NameValuePair> params = new ArrayList<>(3);

        params.add(new BasicNameValuePair("token", token));
        params.add(new BasicNameValuePair("client_id", _clientId));
        params.add(new BasicNameValuePair("client_secret", _clientSecret));

        post.setEntity(new UrlEncodedFormEntity(params));

        return _httpClient.execute(post, response -> {
            if (response.getCode() != HttpStatus.SC_OK)
            {
                _logger.severe(() -> "Got error from introspection server: " + response.getCode());

                throw new IOException("Got error from introspection server: " + response.getCode());
            }

            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } );
    }

    @Override
    public void close() throws IOException
    {
        if (_httpClient instanceof Closeable)
        {
            ((Closeable) _httpClient).close();
        }
    }
}
