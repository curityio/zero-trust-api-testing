package se.curity.examples.spark;

import org.junit.jupiter.api.Test;
import se.curity.examples.exceptions.AuthorizationException;
import se.curity.examples.exceptions.NotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GetProductRequestHandlerTest {

    final static String PREMIUM_SUBSCRIPTION = "premium";
    static ProductServiceTestImpl testProductService = new ProductServiceTestImpl();
    static GetProductRequestHandler getProductRequestHandler = new GetProductRequestHandler(testProductService);

    @Test
    void accessDeniedIfNoCountry() {
        assertThrows(AuthorizationException.class, () -> getProductRequestHandler.getProduct("", PREMIUM_SUBSCRIPTION, "1"));
    }

    @Test
    void accessDeniedIfNoValidSubscription() {
        assertThrows(AuthorizationException.class, () -> getProductRequestHandler.getProduct("se", "", "1"));
    }

    @Test
    void getProduct() throws AuthorizationException, NotFoundException {
        assertEquals(testProductService.getProduct("1"), getProductRequestHandler.getProduct("se", "trial", "1"));
    }

    @Test
    void getExclusiveProduct() throws AuthorizationException, NotFoundException {
        assertEquals(testProductService.getProduct("5"), getProductRequestHandler.getProduct("se", "premium", "5"));
    }

    @Test
    void accessDeniedIfUserIsInWrongCountry() {
        assertThrows(AuthorizationException.class, () -> getProductRequestHandler.getProduct("se", "trial", "2"));
    }

    @Test
    void accessDeniedIfUserHasWrongSubscription() {
        assertThrows(AuthorizationException.class, () -> getProductRequestHandler.getProduct("se", "trial", "5"));
    }

    @Test
    void productNotFound() {
        assertThrows(NotFoundException.class, () -> getProductRequestHandler.getProduct("se", "trial", "6"));
    }



}
