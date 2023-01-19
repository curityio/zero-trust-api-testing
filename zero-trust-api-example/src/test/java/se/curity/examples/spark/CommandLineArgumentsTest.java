package se.curity.examples.spark;


import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import spark.Spark;

import javax.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Ignore
public class CommandLineArgumentsTest {

    @Test
    void testCommandLineArgumentIssuer() {
        String[] args = {"--issuer", "some value"};
        assertDoesNotThrow(() -> new ServerOptions(args));
    }
    @Test
    void testCommandLineArgumentInvalidIssuer() {
        String[] args = {"--issuer", ""};
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(args));
    }

    @Test
    void testCommandLineArgumentPort() {
        String[] args = {"--port", "9090"};
        assertDoesNotThrow(() -> new ServerOptions(args));
    }

    @Test
    void testCommandLineArgumentInvalidPort() {
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(new String[]{"--port", "aaa"}));
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(new String[]{"--port", ""}));
    }

    @Test
    void testCommandLineArgumentJwksURL() {
        String[] args = {"--jwksurl", "https://localhost:8443/somejwksurl"};
        assertDoesNotThrow(() -> new ServerOptions(args));
    }

    @Test
    void testCommandLineArgumentInvalidJwksURL() {
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(new String[]{"--jwksurl", ""}));
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(new String[]{"--jwksurl", "ThisStringIsNotAValidURL"}));
    }

    @Test
    void testCommandLineArgumentAudience() {
        String[] args = {"--audience", "someClientId"};
        assertDoesNotThrow(() -> new ServerOptions(args));
        assertEquals(args[1], new ServerOptions(args).getAudience());
    }

    @Test
    void testInvalidMultipleCommandLineArguments() {
        String[] args = {"--issuer", "some value", "--jwksurl", "urn:not-a-url", "--port", "123"};
        assertThrows(IllegalArgumentException.class, () -> new ServerOptions(args));
    }

    @Test
    void testMultipleCommandLineArguments() {
        String[] args = {"--issuer", "some value", "--jwksurl", "https://this-is-a-valid/jwks-url", "--port", "123"};

        ServerOptions clo = new ServerOptions(args);
        assertEquals( "some value", clo.getIssuer(), "read command line argument --issuer");
        assertEquals("this-is-a-valid", clo.getJwksHost(), "read command line argument --jwksurl (host)");
        assertEquals("443", clo.getJwksPort(), "read command line argument --jwksurl (port)");
        assertEquals("/jwks-url", clo.getJwksPath(), "read command line argument --j2ksurl (path)");
        assertEquals(123, clo.getPort(), "read command line argument --port");

        String [] argsReordered = new String[]{"--jwksurl", "https://this-is-a-valid/jwks-url", "--port", "123", "--issuer", "some value", };
        ServerOptions clo2 = new ServerOptions(argsReordered);
        assertEquals("some value", clo2.getIssuer(), "read command line argument --issuer");
        assertEquals("this-is-a-valid", clo2.getJwksHost(), "read command line argument --jwksurl (host)");
        assertEquals( "443", clo2.getJwksPort(), "read command line argument --jwksurl (port)");
        assertEquals("/jwks-url", clo2.getJwksPath(), "read command line argument --j2ksurl (path)");
        assertEquals(123, clo2.getPort(), "read command line argument --port");
    }

    @Test
    void testStartServerEmptyArguments() throws ServletException {
        String[] emptyArgs = {};
        SparkServerExample.main(emptyArgs);
        Spark.stop();
    }

    @Test
    void testStartServer() throws ServletException {
        String[] args = {"--issuer", "some value", "--jwksurl", "https://this-is-a-valid/jwks-url", "--port", "123"};
        SparkServerExample.main(args);
        Spark.stop();
    }

    @Test
    void testFailedStartServer() {
        String[] invalidArgs = {"--some-argument"};
        assertThrows(IllegalArgumentException.class, () -> {
            SparkServerExample.main(invalidArgs);
            Spark.stop();
        });
    }
}
