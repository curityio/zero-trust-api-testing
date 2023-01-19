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
                    .forEach(product -> {
                        jsonArrayBuilder.add(getJsonObject(product, false));
                    });
        }

        return jsonArrayBuilder.build();
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String countryCode = ((AuthenticatedUser)request.attribute(OAuthFilter.PRINCIPAL_ATTRIBUTE_NAME))
                .getClaims().getString(CLAIM_NAME_COUNTRY);
        return getProducts(countryCode);
    }
}
