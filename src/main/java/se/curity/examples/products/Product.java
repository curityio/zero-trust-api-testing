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

import javax.json.JsonObject;
import java.util.Collection;

public class Product {

    private final String id;
    private final String name;
    private final String description;
    private final boolean isExclusive;
    private final Collection<String> authorizedCountries ;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDetails() {
        return description;
    }

    public boolean IsExclusive() {
        return isExclusive;
    }

    public Collection<String> getAuthorizedCountries() {
        return authorizedCountries;
    }

    public Product(String id, String name, String description, Collection<String> authorizedCountries) {
        this(id, name, description, authorizedCountries, false);
    }

    public Product(String id, String name, String description, Collection<String> authorizedCountries , boolean isExclusive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.authorizedCountries = authorizedCountries;
        this.isExclusive = isExclusive;
    }
}
