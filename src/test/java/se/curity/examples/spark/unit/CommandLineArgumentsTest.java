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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import se.curity.examples.spark.ServerOptions;
import se.curity.examples.spark.SparkServerExample;
import spark.Spark;

import javax.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled
public class CommandLineArgumentsTest {

    @Test
    public void testCommandLineArgumentIssuer() {
        String[] args = {"--issuer", "some value"};
        assertDoesNotThrow(() -> new ServerOptions(args));
    }
    @Test
    public void testCommandLineArgumentInvalidIssuer() {
        String[] args = {"--issuer", ""};
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(args));
    }

    @Test
    public void testCommandLineArgumentPort() {
        String[] args = {"--port", "9090"};
        assertDoesNotThrow(() -> new ServerOptions(args));
    }

    @Test
    public void testCommandLineArgumentInvalidPort() {
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(new String[]{"--port", "aaa"}));
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(new String[]{"--port", ""}));
    }

    @Test
    public void testCommandLineArgumentJwksURL() {
        String[] args = {"--jwksurl", "https://localhost:8443/somejwksurl"};
        assertDoesNotThrow(() -> new ServerOptions(args));
    }

    @Test
    public void testCommandLineArgumentInvalidJwksURL() {
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(new String[]{"--jwksurl", ""}));
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(new String[]{"--jwksurl", "ThisStringIsNotAValidURL"}));
    }

    @Test
    public void testCommandLineArgumentAudience() {
        String[] args = {"--audience", "someClientId"};
        assertDoesNotThrow(() -> new ServerOptions(args));
        assertEquals(args[1], new ServerOptions(args).getAudience());
    }

    @Test
    public void testInvalidMultipleCommandLineArguments() {
        String[] args = {"--issuer", "some value", "--jwksurl", "urn:not-a-url", "--port", "123"};
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(args));
    }

    @Test
    public void testMultipleCommandLineArguments() {
        String[] args = {"--issuer", "some value", "--jwksurl", "https://this-is-a-valid/jwks-url", "--port", "123"};

        ServerOptions clo = new ServerOptions(args);
        assertEquals( "some value", clo.getIssuer(), "read command line argument --issuer");
        assertEquals("this-is-a-valid", clo.getJwksHost(), "read command line argument --jwksurl (host)");
        assertEquals("443", clo.getJwksPort(), "read command line argument --jwksurl (port)");
        assertEquals("/jwks-url", clo.getJwksPath(), "read command line argument --jwksurl (path)");
        assertEquals(123, clo.getPort(), "read command line argument --port");

        String [] argsReordered = new String[]{"--jwksurl", "https://this-is-a-valid/jwks-url", "--port", "123", "--issuer", "some value", };
        ServerOptions clo2 = new ServerOptions(argsReordered);
        assertEquals("some value", clo2.getIssuer(), "read command line argument --issuer");
        assertEquals("this-is-a-valid", clo2.getJwksHost(), "read command line argument --jwksurl (host)");
        assertEquals( "443", clo2.getJwksPort(), "read command line argument --jwksurl (port)");
        assertEquals("/jwks-url", clo2.getJwksPath(), "read command line argument --jwksurl (path)");
        assertEquals(123, clo2.getPort(), "read command line argument --port");
    }

    @Test
    public void testStartServerEmptyArguments() throws ServletException {
        String[] emptyArgs = {};
        startServer(emptyArgs);
    }

    @Test
    public void testStartServer() throws ServletException {
        String[] args = {"--issuer", "some value", "--jwksurl", "https://this-is-a-valid/jwks-url", "--port", "123"};
        startServer(args);
    }

    @Test
    public void testFailedStartServer() {
        String[] invalidArgs = {"--some-argument"};
        assertThrows(IllegalArgumentException.class, () -> {
            startServer(invalidArgs);
        });
    }

    /**
     * Starts server in a controlled fashion: <br/>
     * 1. stops the server
     * 2. starts the server
     * 3. stops the server again
     * @param args
     * @throws ServletException
     */
    private static void startServer(String[] args) throws ServletException {
        Spark.awaitStop();
        SparkServerExample.main(args);
        Spark.awaitStop();
    }
}
