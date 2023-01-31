# OAuth - authentication using access tokens, Spark Example

This project is an example of a Java web application configured to use an
`OAuth Filter` protecting access to the API. It shows, how to enforce a JWT
access token on (certain) requests and use the claims from it for authorization.
In addition, the example includes tests to demonstrate how to effectively work
with authorization and JWTs during development.

The application is a simple API created with [Spark](http://sparkjava.com),
but it could be built using any framework which supports Java Servlets. 
It includes hardcoded data to visualize the results of the authorization rules.

## Building

To build this project, use Maven and run the following command:

```
mvn package
```

This will create a JAR file in the `target` directory. This file is ready to be deployed,
not requiring any external dependencies.

## Deploying

You can run this application stand-alone without any application server with the help of the JAR file:

```bash
java -jar zero-trust-api-example-3.0.0.jar
```

Note, that the application connects to the JWKS URI of the authentication server that - according to the OpenID Connect specification - must be served over HTTPS (see also [Configuration](#configuring)). 
Consequently, this application must trust the certificate presented by the authentication server.

You can use JVM arguments to explicitly set a truststore with the HTTPS certificate of the JWKS URI. Study the following command:

```bash
java -Djavax.net.ssl.trustStore=/path/to/truststore.jks -Djavax.net.ssl.trustStorePassword=changeit -jar zero-trust-api-example-3.0.0.jar
```

## Configuring
As mentioned, this application makes use of [Curity's OAuth filter for Java](https://github.com/curityio/oauth-filter-for-java) to enforce the presentation of JWT bearer tokens in requests.
This filter fetches the keys to validate tokens from the JWKS URI of the authentication server.
By default, it uses a safe default HTTP client for the connection. However, you can specify a custom `HttpClient` that the filter should use to connect to the authentication server instead. 
This is done in the `src/main/resources/META-INF/services/io.curity.oauth.HttpClientProvider` file. This file should contain the name of the class that implements the `HttpClientProvider` interface that creates the custom HTTP client.
See also Java reference documentation for [service loaders](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ServiceLoader.html). 
Study the tests for an example of an `HttpClientProvider`.

You can configure the application with the parameters required for the JWT validation as part of the deployment:

| Command Line Argument | Default Value                                           | Description                                                                                       |
|-----------------------|---------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| --port                | `9090`                                                  | Port number that the example application should listen at                                         |
| --issuer              | `https://localhost:8443/oauth/v2/oauth-anonymous`       | Expected value of `iss` claim in the JWT                                                          |
| --audience            | `www`                                                   | Expected value of `aud` claim in the JWT                                                          |
| --scope               | `read`                                                  | Expected scope in the JWT                                                                         |
| --jwksurl             | `https://localhost:8443/oauth/v2/oauth-anonymous/jwks`  | Endpoint at the authentication server that serves the keys required to validate the JWT signature |


```bash
java -jar zero-trust-api-example-3.0.0.jar --port "9090" --issuer "https://localhost:8443/oauth/v2/oauth-anonymous" --audience "www" --scope "read" --jwksurl "https://localhost:8443/oauth/v2/oauth-anonymous/jwks"
```

## Testing

Once the server is running, you can access the application with your favourite browser via the following URL:

```
http://localhost:9090/
```

The root path is unprotected. 
Other endpoints of this example are `/api/products` and `/api/products/<1-5>`, which all require authentication. 
If the request to those endpoints lacks a valid JWT token, then the server will return `401`. 

The authorization rules use the claims from the access token, namely the `country` and `subscription_level` claims. 
The endpoint `/api/products` returns a list of products depending on the `country` claim in the access token. The list may be empty. 
The endpoint `/api/products/<1-5>` returns the product details if the user has a valid (non-empty) `subscription_level` claim. Exclusive products (`2`,`5`) require a `premium` subscription. 
Checkout the [Working With Claims Tutorial](https://curity.io/resources/learn/working-with-claims/) for how to configure claims for access tokens in the Curity Identity Server.

If the user is not authorized to access the resource, i.e. the JWT is missing a valid subscription level or the user tries to access a product from a different country, then the server will return `403`. 
If the resource cannot be found, e.g. the product with the given ID does not exist, then the server will return `404`. 
Have a look at `se/curity/examples/products/ProductServiceMapImpl.java` for the details of the provided example data.

## More Information

For more information about the Curity Identity Server, please contact [Curity](https://curity.io). 
You can find more examples, tutorials and articles in [Curity's Resource Library](https://curity.io/resources). 

Copyright 2023 Curity AB