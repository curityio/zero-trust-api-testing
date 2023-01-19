package se.curity.examples.spark;

import io.curity.oauth.AuthenticatedUser;
import io.curity.oauth.OAuthFilter;
import se.curity.examples.exceptions.AuthorizationException;
import se.curity.examples.exceptions.NotFoundException;
import spark.Request;
import spark.Response;

import javax.json.JsonObject;

import static spark.Spark.exception;

public class GetProductRequestHandler extends ProductRequestHandler {

    public GetProductRequestHandler(ProductService productService) {
        super(productService);
    }

    public JsonObject getJsonProduct(String countryCode, String subscriptionLevel, String productId) throws AuthorizationException, NotFoundException {

        return getJsonObject(getProduct(countryCode, subscriptionLevel, productId), true);
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        exception(AuthorizationException.class, (error, req, resp) -> {
            resp.status(403);
            resp.body(error.getMessage());
        });
        exception(NotFoundException.class, (error, req, resp) -> {
            resp.status(404);
        });

        String subscriptionLevel;
        String countryCode;
        try {
            subscriptionLevel = ((AuthenticatedUser)request.attribute(OAuthFilter.PRINCIPAL_ATTRIBUTE_NAME))
                           .getClaims().getString(CLAIM_NAME_SUBSCRIPTION_LEVEL);

        } catch (NullPointerException | ClassCastException invalidClaim) {
            // Something is wrong with the subscription. Deny access.
            throw new AuthorizationException("Invalid subscription");
        }

        try {
            countryCode = ((AuthenticatedUser)request.attribute(OAuthFilter.PRINCIPAL_ATTRIBUTE_NAME))
                    .getClaims().getString(CLAIM_NAME_COUNTRY);

        } catch (NullPointerException | ClassCastException invalidClaim) {
            // User is not authorized to view any products
            throw new AuthorizationException();
        }

        return getJsonProduct(countryCode, subscriptionLevel, request.params(":productId"));
    }
}
