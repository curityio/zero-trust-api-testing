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

import java.util.Collection;

/**
 * These are the service offers
 */
public interface ProductService {
    /**
     * Get a product by an id
     * @param id identifier of the product
     * @return product with the given id
     */
    Product getProduct(String id);

    /**
     * Get a list of products offered by the service
     * @return collection of products
     */
    Collection<Product> getProducts();

    /**
     * Check if the given product exists.
     * @param id identifier of the product
     * @return true if the service has a product with the given id, false if the service cannot identify the product
     */
    boolean productExists(String id);
}
