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
package se.curity.examples.spark.unit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import se.curity.examples.exceptions.AuthorizationException;
import se.curity.examples.exceptions.NotFoundException;
import se.curity.examples.products.GetProductRequestHandler;
import se.curity.examples.spark.mock.MockProductServiceImpl;
import spark.Spark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GetProductRequestHandlerTest {

    static final String PREMIUM_SUBSCRIPTION = "premium";
    static final String TRIAL_SUBSCRIPTION = "trial";
    static final MockProductServiceImpl testProductService = new MockProductServiceImpl();
    static final GetProductRequestHandler getProductRequestHandler = new GetProductRequestHandler(testProductService);

    @Test
    void accessDeniedIfNoCountry() {
        assertThrows(AuthorizationException.class, () -> getProductRequestHandler.getProduct("", PREMIUM_SUBSCRIPTION, "1"));
    }
    @Test
    void notFoundIfValidSubscription() {
        assertThrows(NotFoundException.class, () -> getProductRequestHandler.getProduct("", PREMIUM_SUBSCRIPTION, "-1"));
    }

    @Test
    void accessDeniedIfNoValidSubscription() {
        assertThrows(AuthorizationException.class, () -> getProductRequestHandler.getProduct("se", "", "-1"));
    }

    @Test
    void getProductIfAuthorized() throws AuthorizationException, NotFoundException {
        assertEquals(testProductService.getProduct("1"), getProductRequestHandler.getProduct("se", TRIAL_SUBSCRIPTION, "1"));
    }

    @Test
    void getExclusiveProductIfAuthorized() throws AuthorizationException, NotFoundException {
        assertEquals(testProductService.getProduct("5"), getProductRequestHandler.getProduct("se", PREMIUM_SUBSCRIPTION, "5"));
    }

    @Test
    void accessDeniedIfUserIsInWrongCountry() {
        assertThrows(AuthorizationException.class, () -> getProductRequestHandler.getProduct("se", TRIAL_SUBSCRIPTION, "2"));
    }

    @Test
    void accessDeniedIfUserHasWrongSubscription() {
        assertThrows(AuthorizationException.class, () -> getProductRequestHandler.getProduct("se", TRIAL_SUBSCRIPTION, "5"));
    }

    @Test
    void productNotFoundIfAuthorized() {
        assertThrows(NotFoundException.class, () -> getProductRequestHandler.getProduct("se", TRIAL_SUBSCRIPTION, "-1"));
    }

    @AfterAll
    public static void cleanUp() {
        Spark.awaitStop();
    }


}
