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
package se.curity.examples.products;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import se.curity.examples.spark.OAuthFilter;
import spark.Request;
import spark.Response;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

/**
 * A request handler that returns a list of products
 */
public class ListProductsRequestHandler extends ProductRequestHandler {

    public ListProductsRequestHandler(ProductService productService) {
        super(productService);
    }

    @Override
    public Object handle(Request request, Response response) {

        try {
            JwtClaims claimsPrincipal = request.attribute(OAuthFilter.CLAIMS_PRINCIPAL);
            String countryCode = claimsPrincipal.getStringClaimValue(CLAIM_NAME_COUNTRY);
            return getProducts(countryCode);

        } catch (MalformedClaimException exception) {
            // There's an error with the country claim. Return empty list.
            return "[]";
        }
    }

    /**
     * Get the list of products available in the given country
     * @param countryCode country code formatted as ISO3166-1 alpha-2
     * @return the list of products available in the given country or empty list if there are none.
     */
    public JsonArray getProducts(String countryCode) {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        if (!(countryCode == null || countryCode.isBlank())) {
            filterProducts(countryCode)
                    .forEach(product ->
                            jsonArrayBuilder.add(getJsonObject(product, false))
                    );
        }

        return jsonArrayBuilder.build();
    }
}
