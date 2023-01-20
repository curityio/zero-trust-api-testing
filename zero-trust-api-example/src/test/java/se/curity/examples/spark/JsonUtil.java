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

import se.curity.examples.products.Product;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Collection;

public class JsonUtil {

    static JsonObject getJsonObject(Product product) {
        return getJsonObject(product, false);
    }

    static JsonObject getJsonObjectWithDescription(Product product) {
        return getJsonObject(product, true);
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

    static JsonArray getJsonArrayFromCollection(Collection<Product> productList) {
        JsonArrayBuilder jsonProductListBuilder = Json.createArrayBuilder();
        productList.forEach(product -> jsonProductListBuilder.add(
                getJsonObject(product, false)
        ));
        return jsonProductListBuilder.build();
    }

}
