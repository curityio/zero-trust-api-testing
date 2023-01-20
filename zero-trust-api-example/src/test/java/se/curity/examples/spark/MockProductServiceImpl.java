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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of the product service that uses a hash map to store the products
 */
public class MockProductServiceImpl implements ProductService {

    private final ConcurrentHashMap<String, Product> productMap;


    public MockProductServiceImpl() {
        productMap = createProductList();
    }

    @Override
    public Product getProduct(String id) {
        return productMap.get(id);
    }

    @Override
    public Collection<Product> getProducts() {
        return productMap.values();
    }

    @Override
    public boolean productExists(String id) {
        return productMap.containsKey(id);
    }

    public Collection<Product> getProductForCountry(String country) {
        return productMap.values()
                .stream()
                .filter(product -> product.getAuthorizedCountries().contains(country))
                .toList();
    }
    private ConcurrentHashMap<String, Product> createProductList() {
        ConcurrentHashMap<String, Product> productList = new ConcurrentHashMap<>();
        productList.put("1", new Product("1", "Wireless Keyboard", "Wireless keyboard with multimedia hotkeys, comfortable design with a good typing experience. Works for Windows, MacOS and Linux.", List.of("se", "us")));
        productList.put("2", new Product("2", "Wireless On-Ear Headphones", "High-performance bluetooth headphones with soft ear cups and long battery life. Works with iOS and Android devices.", List.of("us"), true));
        productList.put("3", new Product("3", "Screen Protector", "Extra thin and seamless layer to protect the screen of the phone. Does not fit for every phone. ", List.of("se")));
        productList.put("4", new Product("4", "Screen Protector and Privacy Filter 2 in 1", "Extra thin and seamless layer that protects the screen of the phone from scratches. The built-in filter prevents shoulder surfing.", List.of("de")));
        productList.put("5", new Product("5", "Fitness and Health Tracker", "Lightweight accessory for the health conscious. It can monitor skin temperature and heartbeat.", List.of("se"), true));
        return productList;
    }
}
