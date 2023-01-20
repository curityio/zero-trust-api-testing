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

import io.curity.oauth.AuthenticatedUser;
import io.curity.oauth.OAuthFilter;
import se.curity.examples.exceptions.AuthorizationException;
import se.curity.examples.exceptions.NotFoundException;
import spark.Request;
import spark.Response;

import javax.json.JsonObject;

import static spark.Spark.exception;

public class GetProductRequestHandler extends ProductRequestHandler {

    public GetProductRequestHandler(ProductService productService) {
        super(productService);
    }

    public JsonObject getJsonProduct(String countryCode, String subscriptionLevel, String productId) throws AuthorizationException, NotFoundException {

        return getJsonObject(getProduct(countryCode, subscriptionLevel, productId), true);
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        exception(AuthorizationException.class, (error, req, resp) -> {
            resp.status(403);
            resp.body(error.getMessage());
        });
        exception(NotFoundException.class, (error, req, resp) -> resp.status(404));

        String subscriptionLevel;
        String countryCode;
        try {
            subscriptionLevel = ((AuthenticatedUser)request.attribute(OAuthFilter.PRINCIPAL_ATTRIBUTE_NAME))
                           .getClaims().getString(CLAIM_NAME_SUBSCRIPTION_LEVEL);

        } catch (NullPointerException | ClassCastException invalidClaim) {
            // Something is wrong with the subscription. Deny access.
            throw new AuthorizationException("Invalid subscription");
        }

        try {
            countryCode = ((AuthenticatedUser)request.attribute(OAuthFilter.PRINCIPAL_ATTRIBUTE_NAME))
                    .getClaims().getString(CLAIM_NAME_COUNTRY);

        } catch (NullPointerException | ClassCastException invalidClaim) {
            // User is not authorized to view any products
            throw new AuthorizationException();
        }

        return getJsonProduct(countryCode, subscriptionLevel, request.params(":productId"));
    }
}
