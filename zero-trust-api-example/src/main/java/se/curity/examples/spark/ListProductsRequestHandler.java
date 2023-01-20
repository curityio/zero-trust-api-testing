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
import spark.Request;
import spark.Response;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

public class ListProductsRequestHandler extends ProductRequestHandler {

    public ListProductsRequestHandler(ProductService productService) {
        super(productService);
    }

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

    @Override
    public Object handle(Request request, Response response) {
        try {
            String countryCode = ((AuthenticatedUser)request.attribute(OAuthFilter.PRINCIPAL_ATTRIBUTE_NAME))
                    .getClaims().getString(CLAIM_NAME_COUNTRY);
            return getProducts(countryCode);
        } catch (NullPointerException npe) {
            // claim was not found. Return empty list.
            return JsonArray.EMPTY_JSON_ARRAY;
        }
    }
}
