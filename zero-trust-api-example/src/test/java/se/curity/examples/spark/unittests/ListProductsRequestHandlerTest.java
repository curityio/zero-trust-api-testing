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
 */package se.curity.examples.spark.unittests;

import org.junit.jupiter.api.Test;
import se.curity.examples.products.Product;
import se.curity.examples.spark.ListProductsRequestHandler;
import se.curity.examples.spark.mock.MockProductServiceImpl;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class ListProductsRequestHandlerTest {

    static final MockProductServiceImpl testProductService = new MockProductServiceImpl();
    static final ListProductsRequestHandler listProductsRequestHandler = new ListProductsRequestHandler(testProductService);

    @Test
    void emptyListIfCountryIsMissing() {
        assertTrue(listProductsRequestHandler.getProducts(null).isEmpty(),"Empty product list when country is missing.");
        assertTrue(listProductsRequestHandler.getProducts("").isEmpty(), "Empty product list when country is empty.");
    }

    @Test
    void filterProductListOnCountry() {
        JsonArray filteredForCountry = toJsonArray(testProductService.getProductForCountry("se"));
        assertEquals(filteredForCountry, listProductsRequestHandler.getProducts("se"));
    }

    private JsonArray toJsonArray(Collection<Product> productList) {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        productList.forEach(product -> jsonArrayBuilder.add(toJsonObject(product)));
        return jsonArrayBuilder.build();
    }

    private JsonObject toJsonObject(Product product) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                .add("id", product.getId())
                .add("name", product.getName())
                .add("isExclusive", product.IsExclusive());

        return objectBuilder.build();
    }

    //TODO: add tests for countries
    //TODO: add tests for specific products
}