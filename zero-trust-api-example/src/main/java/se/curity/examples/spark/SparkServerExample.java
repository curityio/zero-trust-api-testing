/*
 * Copyright (C) 2016 Curity AB.
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

import io.curity.oauth.OAuthFilter;
import io.curity.oauth.OAuthJwtFilter;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.servlet.SparkApplication;

import javax.annotation.Nullable;
import javax.servlet.ServletException;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.redirect;

public class SparkServerExample implements SparkApplication
{
    private static final Logger _logger = LoggerFactory.getLogger(SparkServerExample.class);

    /**
     * Start the server with the given product service and options.
     * This sets up the routes for the product service and makes sure that the routes are protected by OAuth.
     * @param productService
     */
    public SparkServerExample(ProductService productService, @Nullable ServerOptions options) throws ServletException {
        ServerOptions appliedOptions;

        if (options == null) {
            appliedOptions = new ServerOptions();
        } else {
            appliedOptions = options;
        }

        port(appliedOptions.getPort());

        initStandalone(appliedOptions);
        // Set up the product service to respond to /products and /products/productId routes
        path("/api", () -> {
            before("/*", (q, a) -> _logger.debug("Received api call"));
            path("/products", () -> {
                get("", new ListProductsRequestHandler(productService));
                get("/:productId", new GetProductRequestHandler(productService));
            });
        });

    }

    @Override
    /**
     * Set up some basic routes.
     */
    public void init()
    {
        _logger.debug("Initializing OAuth protected API");

        redirect.get("", "/");
        get("/", ((request, response) -> {
           return "Welcome!";
        }));
    }

    private void initStandalone(ServerOptions options) throws ServletException
    {
        init();
        OAuthFilter filter = getJwtFilter(options);

        Filter oauthFilter = (request, response) -> {
            filter.doFilter(request.raw(), response.raw(), null);
            if(response.raw().isCommitted())
            {
                halt();
            }
        };
        // Run the filter before any api/* route
        before("/api", oauthFilter);
        before("/api/", oauthFilter);
        before("/api/*", oauthFilter);

    }

    /**
     * Start the server locally with the given product service and options
     * @param productService the service that provides the data
     * @param options the options such as port, issuer, audience and jwksurl
     * @throws ServletException
     */
    public static void runLocally(@Nullable ProductService productService, @Nullable ServerOptions options) throws ServletException {

        ProductService productServiceInUse;
        ServerOptions optionsToApply;

        SparkServerExample server;

        if (productService == null) {
            productServiceInUse = new ProductServiceMapImpl();
        } else {
            productServiceInUse = productService;
        }

        if (options == null) {
            optionsToApply = new ServerOptions();
        } else {
            optionsToApply = options;
        }

        new SparkServerExample(productServiceInUse, optionsToApply);
    }

    public static void main(String[] args) throws ServletException {

        ServerOptions options = new ServerOptions(args);
        new SparkServerExample(new ProductServiceMapImpl(), options);
    }

    /**
     * Configure the OAuth Filter and return it.
     * @param options the issuer, audience, scope and jwksurl
     * @return the OAuth filter that will do some basic JWT validation and verification
     * @throws ServletException
     */
    private OAuthFilter getJwtFilter(ServerOptions options) throws ServletException
    {
        EmbeddedSparkJwtFilterConfig filterParams = new EmbeddedSparkJwtFilterConfig(options.getJwksHost(),
                options.getJwksPort(),
                options.getJwksPath(),
                options.getScope(),
                "3600",
                options.getIssuer(),
                options.getAudience());
        OAuthFilter filter = new OAuthJwtFilter();

        filter.init(filterParams);
        return filter;
    }

}
