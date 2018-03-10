/*
 * The MIT License
 *
 * Copyright 2017 EXT-acourt.
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
package fr.askjadev.xml.extfunctions.marklogic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.admin.ExtensionLibrariesManager;
import com.marklogic.client.admin.ExtensionLibraryDescriptor;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.InputStreamHandle;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.expr.EarlyEvaluationContext;
import net.sf.saxon.jaxp.TransformerImpl;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.BigIntegerValue;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.StringValue;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;

/**
 * Query invoke test NB: user needs to be rest-admin in MarkLogic
 *
 * @author Emmanuel Tourdot
 */
public class MarkLogicQueryInvokeTest {

    private HashTrieMap CONNECT;
    private DatabaseClient client;
    private ExtensionLibrariesManager librariesManager;
    private Configuration configuration;
    private Processor processor;
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setup() throws XPathException {
        String server = System.getProperty("testServer") == null ? "localhost" : System.getProperty("testServer");
        Integer port = System.getProperty("testPort") == null ? 8004 : Integer.parseInt(System.getProperty("testPort"));
        String user = System.getProperty("testUser") == null ? "admin" : System.getProperty("testUser");
        String password = System.getProperty("testPassword") == null ? "admin" : System.getProperty("testPassword");
        this.configuration = new Configuration();
        this.processor = new Processor(configuration);
        EarlyEvaluationContext xpathContext = new EarlyEvaluationContext(configuration);
        HashTrieMap serverConfig = new HashTrieMap(xpathContext); 
        serverConfig = serverConfig.addEntry(new StringValue("server"), new StringValue(System.getProperty("testServer") == null ? "localhost" : System.getProperty("testServer")));
        serverConfig = serverConfig.addEntry(new StringValue("port"), (IntegerValue) new BigIntegerValue(System.getProperty("testPort") == null ? 8004 : Integer.parseInt(System.getProperty("testPort"))));
        serverConfig = serverConfig.addEntry(new StringValue("user"), new StringValue(System.getProperty("testUser") == null ? "admin" : System.getProperty("testUser")));
        serverConfig = serverConfig.addEntry(new StringValue("password"), new StringValue(System.getProperty("testPassword") == null ? "admin" : System.getProperty("testPassword")));
        this.CONNECT = serverConfig;
        client = DatabaseClientFactory.newClient(server, port, new DatabaseClientFactory.BasicAuthContext(user, password));
        librariesManager = client.newServerConfigManager().newExtensionLibrariesManager();
        ExtensionLibraryDescriptor moduleDescriptor1 = new ExtensionLibraryDescriptor();
        moduleDescriptor1.setPath("/ext/test/MarkLogicQueryInvokeTest.xqy");
        InputStreamHandle xquery1 = new InputStreamHandle(this.getClass().getClassLoader().getResourceAsStream("MarkLogicQueryInvokeTest.xqy"));
        xquery1.setFormat(Format.TEXT);
        librariesManager.write(moduleDescriptor1, xquery1);
        ExtensionLibraryDescriptor moduleDescriptor2 = new ExtensionLibraryDescriptor();
        moduleDescriptor2.setPath("/ext/test/MarkLogicQuery_ExternalVariables.xqy");
        InputStreamHandle xquery2 = new InputStreamHandle(this.getClass().getClassLoader().getResourceAsStream("MarkLogicQuery_ExternalVariables.xqy"));
        xquery2.setFormat(Format.TEXT);
        librariesManager.write(moduleDescriptor2, xquery2);
    }

    @After
    public void tearDown() {
        if (client != null) {
            librariesManager.delete("/ext/test/MarkLogicQueryInvokeTest.xqy");
            librariesManager.delete("/ext/test/MarkLogicQueryInvokeTest.xqy");
            client.release();
        }
    }
    
    /**
     * Test of getFunctionQName method, of class MarkLogicQueryInvoke.
     */
    @Test
    public void testGetFunctionQName() {
        MarkLogicQueryInvoke instance = new MarkLogicQueryInvoke();
        StructuredQName expResult = new StructuredQName("mkl-ext", "fr:askjadev:xml:extfunctions", "marklogic-query-invoke");
        StructuredQName result = instance.getFunctionQName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getArgumentTypes method, of class MarkLogicQueryInvoke.
     */
    @Test
    public void testGetArgumentTypes() {
        MarkLogicQueryInvoke instance = new MarkLogicQueryInvoke();
        net.sf.saxon.value.SequenceType[] expResult = new net.sf.saxon.value.SequenceType[] { net.sf.saxon.value.SequenceType.SINGLE_STRING, MapType.OPTIONAL_MAP_ITEM, MapType.OPTIONAL_MAP_ITEM };
        net.sf.saxon.value.SequenceType[] result = instance.getArgumentTypes();
        assertEquals(expResult.length, result.length);
        for (int i=0; i<expResult.length; i++) {
            assertEquals("Entry " + i + " differs from expected: ", expResult[i], result[i]);
        }
    }

    /**
     * Test of getMinimumNumberOfArguments method, of class MarkLogicQueryInvoke.
     */
    @Test
    public void testGetMinimumNumberOfArguments() {
        MarkLogicQueryInvoke instance = new MarkLogicQueryInvoke();
        int expResult = 2;
        int result = instance.getMinimumNumberOfArguments();
        assertEquals(expResult, result);
    }

    /**
     * Test of getMaximumNumberOfArguments method, of class MarkLogicQueryInvoke.
     */
    @Test
    public void testGetMaximumNumberOfArguments() {
        MarkLogicQueryInvoke instance = new MarkLogicQueryInvoke();
        int expResult = 3;
        int result = instance.getMaximumNumberOfArguments();
        assertEquals(expResult, result);
    }

    /**
     * Test of getResultType method, of class MarkLogicQueryInvoke.
     */
    @Test
    public void testGetResultType() {
        net.sf.saxon.value.SequenceType[] sts = null;
        MarkLogicQueryInvoke instance = new MarkLogicQueryInvoke();
        net.sf.saxon.value.SequenceType expResult = net.sf.saxon.value.SequenceType.ANY_SEQUENCE;
        net.sf.saxon.value.SequenceType result = instance.getResultType(sts);
        assertEquals(expResult, result);
    }

    /**
     * Test of makeCallExpression method.
     * @throws net.sf.saxon.trans.XPathException
     * @throws net.sf.saxon.s9api.SaxonApiException
     */
    @Test
    public void testInvokeModule2Args() throws XPathException, SaxonApiException {
        configuration.registerExtensionFunction(new MarkLogicQueryInvoke());
        XPathCompiler xpc = processor.newXPathCompiler();
        try {
            xpc.declareNamespace(MarkLogicQueryInvoke.EXT_NS_COMMON_PREFIX, MarkLogicQueryInvoke.EXT_NAMESPACE_URI);
            QName var = new QName("config");
            xpc.declareVariable(var);
            XPathSelector xp = xpc.compile(MarkLogicQueryInvoke.EXT_NS_COMMON_PREFIX + ":" + MarkLogicQueryInvoke.FUNCTION_NAME + "('/ext/test/MarkLogicQueryInvokeTest.xqy', $config)").load();
            XdmValue xqConfig = XdmValue.wrap(CONNECT);
            xp.setVariable(var, xqConfig);
            XdmValue result = xp.evaluate();
            SequenceIterator it = result.getUnderlyingValue().iterate();
            Item item = it.next();
            assertEquals("test", item.getStringValue());
            it.close();
        }
        catch (XPathException | SaxonApiException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Test KO / Argument with wrong type inside config map
     * @throws SaxonApiException
     * @throws net.sf.saxon.trans.XPathException
     */
    @Test(expected = SaxonApiException.class)
    public void testQueryModule2Args_WrongParamType() throws SaxonApiException, XPathException {
        configuration.registerExtensionFunction(new MarkLogicQueryInvoke());
        XPathCompiler xpc = processor.newXPathCompiler();
        try {
            xpc.declareNamespace(MarkLogicQueryInvoke.EXT_NS_COMMON_PREFIX, MarkLogicQueryInvoke.EXT_NAMESPACE_URI);
            QName var = new QName("config");
            xpc.declareVariable(var);
            XPathSelector xp = xpc.compile(MarkLogicQueryInvoke.EXT_NS_COMMON_PREFIX + ":" + MarkLogicQueryInvoke.FUNCTION_NAME + "('/ext/test/MarkLogicQueryInvokeTest.xqy', $config)").load();
            HashTrieMap serverConfig = CONNECT.addEntry(new StringValue("port"), new StringValue("string"));
            XdmValue xqConfig = XdmValue.wrap(serverConfig);
            xp.setVariable(var, xqConfig);
            XdmValue result = xp.evaluate();
        }
        catch (XPathException | SaxonApiException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Test KO / Missing mandatory argument inside config map
     * @throws SaxonApiException
     * @throws net.sf.saxon.trans.XPathException
     */
    @Test(expected = SaxonApiException.class)
    public void testQueryModule2Args_MissingParam() throws SaxonApiException, XPathException {
        configuration.registerExtensionFunction(new MarkLogicQueryInvoke());
        XPathCompiler xpc = processor.newXPathCompiler();
        try {
            xpc.declareNamespace(MarkLogicQueryInvoke.EXT_NS_COMMON_PREFIX, MarkLogicQueryInvoke.EXT_NAMESPACE_URI);
            QName var = new QName("config");
            xpc.declareVariable(var);
            XPathSelector xp = xpc.compile(MarkLogicQueryInvoke.EXT_NS_COMMON_PREFIX + ":" + MarkLogicQueryInvoke.FUNCTION_NAME + "('/ext/test/MarkLogicQueryInvokeTest.xqy', $config)").load();
            HashTrieMap serverConfig = CONNECT.remove(new StringValue("server"));
            XdmValue xqConfig = XdmValue.wrap(serverConfig);
            xp.setVariable(var, xqConfig);
            XdmValue result = xp.evaluate();
        }
        catch (XPathException | SaxonApiException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Test KO / 2nd argument wrong type
     * @throws SaxonApiException
     */
    @Test(expected = SaxonApiException.class)
    public void testQueryModule2Args_BadArgument() throws SaxonApiException {
        configuration.registerExtensionFunction(new MarkLogicQueryInvoke());
        XPathCompiler xpc = processor.newXPathCompiler();
        try {
            xpc.declareNamespace(MarkLogicQueryInvoke.EXT_NS_COMMON_PREFIX, MarkLogicQueryInvoke.EXT_NAMESPACE_URI);
            QName var = new QName("config");
            xpc.declareVariable(var);
            XPathSelector xp = xpc.compile(MarkLogicQueryInvoke.EXT_NS_COMMON_PREFIX + ":" + MarkLogicQueryInvoke.FUNCTION_NAME + "('/ext/test/MarkLogicQueryInvokeTest.xqy', $config)").load();
            XdmAtomicValue xqConfig = new XdmAtomicValue("string");
            xp.setVariable(var, xqConfig);
            XdmValue result = xp.evaluate();
        }
        catch (SaxonApiException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Test OK with XSL
     * @throws XPathException
     * @throws TransformerConfigurationException
     * @throws java.net.URISyntaxException
     */
    @Test
    public void testXSL_QueryOK() throws XPathException, TransformerConfigurationException, URISyntaxException {
        TransformerFactory factory = TransformerFactory.newInstance();
        TransformerFactoryImpl tFactoryImpl = (TransformerFactoryImpl) factory;
        configuration.registerExtensionFunction(new MarkLogicQueryInvoke());
        tFactoryImpl.setConfiguration(configuration);
        try {
            Source xslt = new StreamSource(this.getClass().getClassLoader().getResource("MarkLogicQueryInvokeTest_OK.xsl").toURI().toString());
            TransformerImpl transformer = (TransformerImpl) factory.newTransformer(xslt);
            transformer.setParameter("config", CONNECT);
            Source text = new StreamSource(this.getClass().getClassLoader().getResourceAsStream("MarkLogicQuery_DummySource.xml"));
            StringWriter result = new StringWriter();
            transformer.transform(text, new StreamResult(result));
        }
        catch (XPathException | TransformerConfigurationException | URISyntaxException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Test KO with XSL - XQuery module not found
     * @throws XPathException
     * @throws TransformerConfigurationException
     * @throws java.net.URISyntaxException
     */
    @Test(expected = XPathException.class)
    public void testXSL_QueryKO_FileNotFound() throws XPathException, TransformerConfigurationException, URISyntaxException {
        TransformerFactory factory = TransformerFactory.newInstance();
        TransformerFactoryImpl tFactoryImpl = (TransformerFactoryImpl) factory;
        configuration.registerExtensionFunction(new MarkLogicQueryInvoke());
        tFactoryImpl.setConfiguration(configuration);
        try {
            Source xslt = new StreamSource(this.getClass().getClassLoader().getResource("MarkLogicQueryInvokeTest_ModuleNotFound.xsl").toURI().toString());
            TransformerImpl transformer = (TransformerImpl) factory.newTransformer(xslt);
            transformer.setParameter("config", CONNECT);
            Source text = new StreamSource(this.getClass().getClassLoader().getResourceAsStream("MarkLogicQuery_DummySource.xml"));
            StringWriter result = new StringWriter();
            transformer.transform(text, new StreamResult(result));
        }
        catch (XPathException | TransformerConfigurationException | URISyntaxException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Test OK with XSL + external variables
     * @throws XPathException
     * @throws TransformerConfigurationException
     * @throws java.net.URISyntaxException
     * @throws java.io.IOException
     * @throws net.sf.saxon.s9api.SaxonApiException
     */
    @Test
    public void testXSL_ExternalVar_QueryOK() throws XPathException, TransformerConfigurationException, URISyntaxException, IOException, SaxonApiException {
        TransformerFactory factory = TransformerFactory.newInstance();
        TransformerFactoryImpl tFactoryImpl = (TransformerFactoryImpl) factory;
        configuration.registerExtensionFunction(new MarkLogicQueryInvoke());
        tFactoryImpl.setConfiguration(configuration);
        try {
            Source xslt = new StreamSource(this.getClass().getClassLoader().getResource("MarkLogicQueryInvokeTest_ExternalVariables_OK.xsl").toURI().toString());
            TransformerImpl transformer = (TransformerImpl) factory.newTransformer(xslt);
            transformer.setParameter("config", CONNECT);
            Source text = new StreamSource(this.getClass().getClassLoader().getResourceAsStream("MarkLogicQuery_DummySource.xml"));
            StringWriter result = new StringWriter();
            transformer.transform(text, new StreamResult(result));
            // System.out.println(result.toString());
            DocumentBuilder builder = processor.newDocumentBuilder();
            XdmNode resultNode = (XdmNode) builder.build(new StreamSource(IOUtils.toInputStream(result.toString(), "UTF-8")));
            XdmSequenceIterator it = resultNode.axisIterator(Axis.DESCENDANT, new QName("external-variable"));
            while (it.hasNext()) {
                XdmNode element = (XdmNode) it.next();
                assertEquals("true", element.getAttributeValue(new QName("isTypeAsExpected")));
            }
            it.close();
        }
        catch (XPathException | TransformerConfigurationException | URISyntaxException | IOException | SaxonApiException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }

}
