/*
 * The MIT License
 *
 * Copyright 2020 Axel Court.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fr.askjadev.xml.extfunctions.marklogic.config;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import okhttp3.OkHttpClient;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ext-acourt
 */
public class OkHttpClientProxyConfiguratorTest {

    /**
     * Test of configure method, of class OkHttpClientProxyConfigurator.
     * @throws java.net.URISyntaxException
     */
    @Test
    public void testConfigure() throws URISyntaxException {
        // Set the proxy settings
        System.setProperty("http.proxySet", "true");
        System.setProperty("http.proxyHost", "99.99.99.99");
        System.setProperty("http.proxyPort", "80");
        System.setProperty("http.nonProxyHosts", "20.20.20.20");
        // Make a DatabaseClientFactory that will use the proxy settings through a OkHttpClientProxyConfigurator
        OkHttpClientProxyConfigurator configurator = new OkHttpClientProxyConfigurator();
        DatabaseClientFactory.addConfigurator(configurator);
        // Make a DatabaseClient from that factory
        DatabaseClient databaseClient = DatabaseClientFactory.newClient("10.10.10.10", 8999, new DigestAuthContext("user", "pass"));
        try {
            // Custom OkHttpClientProxyConfigurator is used
            assertTrue("DatabaseClientFactory did use the custom OkHttpClientProxyConfigurator.", configurator.isConfigured);
            // Verify that proxy settings are being used
            OkHttpClient okClient = (OkHttpClient) databaseClient.getClientImplementation();
            // Must go through proxy
            assertEquals("99.99.99.99:80", okClient.proxySelector().select(new URI("http://10.10.10.10:8999")).get(0).address().toString());
            // Must not go through proxy
            assertEquals(Proxy.NO_PROXY, okClient.proxySelector().select(new URI("http://20.20.20.20:8999")).get(0));
        }
        finally {
            databaseClient.release();
        }
    }

}
