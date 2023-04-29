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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.examples.products.GetProductRequestHandler;
import se.curity.examples.products.ListProductsRequestHandler;
import se.curity.examples.products.ProductService;
import se.curity.examples.products.ProductServiceMapImpl;
import spark.Filter;
import spark.servlet.SparkApplication;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import java.util.Objects;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.redirect;

public class SparkServerExample implements SparkApplication
{
    private static final Logger _logger = LoggerFactory.getLogger(SparkServerExample.class);

    /**
     * Start the server with the given product service and options.
     * This sets up the routes for the product service and makes sure that the routes are protected by OAuth.
     * @param productService the service that can access the products
     * @param options different options to start the server with
     * @throws ServletException if the oauth filter cannot be initialized
     */
    public SparkServerExample(ProductService productService, @Nullable ServerOptions options) throws ServletException {

        ServerOptions appliedOptions = Objects.requireNonNullElseGet(options, ServerOptions::new);
        port(appliedOptions.getPort());
        init();

        // Run the filter before any api/* route
        Filter oauthFilter = toSparkFilter(new OAuthFilter(options));
        before("/api", oauthFilter);
        before("/api/", oauthFilter);
        before("/api/*", oauthFilter);

        // Set up the product service to respond to /products and /products/productId routes
        path("/api", () ->
                path("/products", () -> {
                    get("", new ListProductsRequestHandler(productService));
                    get("/:productId", new GetProductRequestHandler(productService));
            })
        );
    }

    @Override
    public void init()
    {
        _logger.debug("Initializing OAuth protected API");
        redirect.get("", "/");
        get("/", ((request, response) -> "Welcome!"));
    }

    private Filter toSparkFilter(javax.servlet.Filter filter) {
        Filter sparkFilter = (request, response) -> {
            filter.doFilter(request.raw(), response.raw(), null);
        };
        return sparkFilter;
    }

    public static void main(String[] args) throws ServletException {

        ServerOptions options = new ServerOptions(args);
        new SparkServerExample(new ProductServiceMapImpl(), options);
    }

}
