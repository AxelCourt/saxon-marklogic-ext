/*
 * The MIT License
 *
 * Copyright 2017 Axel Court.
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

import java.io.StringWriter;
import java.net.URISyntaxException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.jaxp.TransformerImpl;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmEmptySequence;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BigIntegerValue;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for MarkLogicQueryURI
 * @author Axel Court
 */
public class MarkLogicQueryURITest {
    
    private final HashTrieMap CONNECT;
    private final Configuration configuration;
    private final Processor processor;
    
    public MarkLogicQueryURITest() throws XPathException {
        super();
        this.configuration = new Configuration();
        this.processor = new Processor(configuration);
        HashTrieMap serverConfig = new HashTrieMap(); 
        serverConfig = serverConfig.addEntry(new StringValue("server"), new StringValue(System.getProperty("testServer") == null ? "localhost" : System.getProperty("testServer")));
        serverConfig = serverConfig.addEntry(new StringValue("port"), (IntegerValue) new BigIntegerValue(System.getProperty("testPort") == null ? 8004 : Integer.parseInt(System.getProperty("testPort"))));
        serverConfig = serverConfig.addEntry(new StringValue("user"), new StringValue(System.getProperty("testUser") == null ? "admin" : System.getProperty("testUser")));
        serverConfig = serverConfig.addEntry(new StringValue("password"), new StringValue(System.getProperty("testPassword") == null ? "admin" : System.getProperty("testPassword")));
        this.CONNECT = serverConfig;
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getFunctionQName method, of class MarkLogicQueryURI.
     */
    @Test
    public void testGetFunctionQName() {
        MarkLogicQueryURI instance = new MarkLogicQueryURI();
        StructuredQName expResult = new StructuredQName("mkl-ext", "fr:askjadev:xml:extfunctions", "marklogic-query-uri");
        StructuredQName result = instance.getFunctionQName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getArgumentTypes method, of class MarkLogicQueryURI.
     */
    @Test
    public void testGetArgumentTypes() {
        MarkLogicQueryURI instance = new MarkLogicQueryURI();
        SequenceType[] expResult = new SequenceType[] { SequenceType.SINGLE_STRING, MapType.OPTIONAL_MAP_ITEM, MapType.OPTIONAL_MAP_ITEM };
        SequenceType[] result = instance.getArgumentTypes();
        assertEquals(expResult.length, result.length);
        for (int i=0; i<expResult.length; i++) {
            assertEquals("Entry " + i + " differs from expected: ", expResult[i], result[i]);
        }
    }

    /**
     * Test of getMinimumNumberOfArguments method, of class MarkLogicQueryURI.
     */
    @Test
    public void testGetMinimumNumberOfArguments() {
        MarkLogicQueryURI instance = new MarkLogicQueryURI();
        int expResult = 2;
        int result = instance.getMinimumNumberOfArguments();
        assertEquals(expResult, result);
    }

    /**
     * Test of getMaximumNumberOfArguments method, of class MarkLogicQueryURI.
     */
    @Test
    public void testGetMaximumNumberOfArguments() {
        MarkLogicQueryURI instance = new MarkLogicQueryURI();
        int expResult = 3;
        int result = instance.getMaximumNumberOfArguments();
        assertEquals(expResult, result);
    }

    /**
     * Test of getResultType method, of class MarkLogicQueryURI.
     */
    @Test
    public void testGetResultType() {
        SequenceType[] sts = null;
        MarkLogicQueryURI instance = new MarkLogicQueryURI();
        SequenceType expResult = SequenceType.ANY_SEQUENCE;
        SequenceType result = instance.getResultType(sts);
        assertEquals(expResult, result);
    }

    /**
     * Test of makeCallExpression method.
     * @throws XPathException
     * @throws SaxonApiException
     */
    @Test
    public void testQueryModule2Args() throws XPathException, SaxonApiException {
        configuration.registerExtensionFunction(new MarkLogicQueryURI());
        XPathCompiler xpc = processor.newXPathCompiler();
        try {
            xpc.declareNamespace(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX, MarkLogicQueryURI.EXT_NAMESPACE_URI);
            QName varConf = new QName("config");
            QName varUri = new QName("xqueryUri");
            xpc.declareVariable(varConf);
            xpc.declareVariable(varUri);
            XPathSelector xp = xpc.compile(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX + ":" + MarkLogicQueryURI.FUNCTION_NAME + "($xqueryUri, $config)").load();
            XdmValue xqConfig = XdmValue.wrap(CONNECT);
            xp.setVariable(varConf, xqConfig);
            String xqueryUri = this.getClass().getClassLoader().getResource("MarkLogicQueryURITest.xqy").toString();
            XdmAtomicValue xqUri = new XdmAtomicValue(xqueryUri);
            xp.setVariable(varUri, xqUri);
            XdmValue result = xp.evaluate();
            SequenceIterator it = result.getUnderlyingValue().iterate();
            Item item = it.next();
            int count = 1;
            while (item != null) {
                assertEquals(Integer.toString(count++), item.getStringValue());
                item = it.next();
            }
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
     * @throws XPathException
     */
    @Test(expected = SaxonApiException.class)
    public void testQueryModule2Args_WrongParamType() throws SaxonApiException, XPathException {
        configuration.registerExtensionFunction(new MarkLogicQueryURI());
        XPathCompiler xpc = processor.newXPathCompiler();
        try {
            xpc.declareNamespace(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX, MarkLogicQueryURI.EXT_NAMESPACE_URI);
            QName varConf = new QName("config");
            QName varUri = new QName("xqueryUri");
            xpc.declareVariable(varConf);
            xpc.declareVariable(varUri);
            XPathSelector xp = xpc.compile(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX + ":" + MarkLogicQueryURI.FUNCTION_NAME + "($xqueryUri, $config)").load();
            HashTrieMap serverConfig = CONNECT.addEntry(new StringValue("port"), new StringValue("string"));
            XdmValue xqConfig = XdmValue.wrap(serverConfig);
            xp.setVariable(varConf, xqConfig);
            String xqueryUri = this.getClass().getClassLoader().getResource("MarkLogicQueryURITest.xqy").toString();
            XdmAtomicValue xqUri = new XdmAtomicValue(xqueryUri);
            xp.setVariable(varUri, xqUri);
            xp.evaluate();
        }
        catch (SaxonApiException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Test KO / Missing mandatory argument inside config map
     * @throws SaxonApiException
     * @throws XPathException
     */
    @Test(expected = SaxonApiException.class)
    public void testQueryModule2Args_MissingParam() throws SaxonApiException, XPathException {
        configuration.registerExtensionFunction(new MarkLogicQueryURI());
        XPathCompiler xpc = processor.newXPathCompiler();
        try {
            xpc.declareNamespace(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX, MarkLogicQueryURI.EXT_NAMESPACE_URI);
            QName varConf = new QName("config");
            QName varUri = new QName("xqueryUri");
            xpc.declareVariable(varConf);
            xpc.declareVariable(varUri);
            XPathSelector xp = xpc.compile(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX + ":" + MarkLogicQueryURI.FUNCTION_NAME + "($xqueryUri, $config)").load();
            HashTrieMap serverConfig = CONNECT.remove(new StringValue("server"));
            XdmValue xqConfig = XdmValue.wrap(serverConfig);
            xp.setVariable(varConf, xqConfig);
            String xqueryUri = this.getClass().getClassLoader().getResource("MarkLogicQueryURITest.xqy").toString();
            XdmAtomicValue xqUri = new XdmAtomicValue(xqueryUri);
            xp.setVariable(varUri, xqUri);
            xp.evaluate();
        }
        catch (SaxonApiException ex) {
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
        configuration.registerExtensionFunction(new MarkLogicQueryURI());
        XPathCompiler xpc = processor.newXPathCompiler();
        try {
            xpc.declareNamespace(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX, MarkLogicQueryURI.EXT_NAMESPACE_URI);
            QName varConf = new QName("config");
            QName varUri = new QName("xqueryUri");
            xpc.declareVariable(varConf);
            xpc.declareVariable(varUri);
            XPathSelector xp = xpc.compile(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX + ":" + MarkLogicQueryURI.FUNCTION_NAME + "($xqueryUri, $config)").load();
            XdmAtomicValue xqConfig = new XdmAtomicValue("string");
            xp.setVariable(varConf, xqConfig);
            String xqueryUri = this.getClass().getClassLoader().getResource("MarkLogicQueryURITest.xqy").toString();
            XdmAtomicValue xqUri = new XdmAtomicValue(xqueryUri);
            xp.setVariable(varUri, xqUri);
            xp.evaluate();
        }
        catch (SaxonApiException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Test KO / 3rd argument has a wrong type
     * @throws SaxonApiException
     */
    @Test(expected = SaxonApiException.class)
    public void testQueryModule2Args_3dArgBadType() throws SaxonApiException {
        configuration.registerExtensionFunction(new MarkLogicQueryURI());
        XPathCompiler xpc = processor.newXPathCompiler();
        try {
            xpc.declareNamespace(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX, MarkLogicQueryURI.EXT_NAMESPACE_URI);
            QName varConf = new QName("config");
            QName varUri = new QName("xqueryUri");
            QName varExtVars = new QName("extVars");
            xpc.declareVariable(varConf);
            xpc.declareVariable(varUri);
            xpc.declareVariable(varExtVars);
            XPathSelector xp = xpc.compile(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX + ":" + MarkLogicQueryURI.FUNCTION_NAME + "($xqueryUri, $config, $extVars)").load();
            XdmValue xqConfig = XdmValue.wrap(CONNECT);
            xp.setVariable(varConf, xqConfig);
            String xqueryUri = this.getClass().getClassLoader().getResource("MarkLogicQueryURITest.xqy").toString();
            XdmAtomicValue xqUri = new XdmAtomicValue(xqueryUri);
            xp.setVariable(varUri, xqUri);
            xp.setVariable(varExtVars, new XdmAtomicValue("string"));
            xp.evaluate();
        }
        catch (SaxonApiException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Test OK / 3rd argument is an empty sequence
     * @throws SaxonApiException
     * @throws XPathException
     */
    @Test
    public void testQueryModule2Args_3dArgEmptySeq() throws SaxonApiException, XPathException {
        configuration.registerExtensionFunction(new MarkLogicQueryURI());
        XPathCompiler xpc = processor.newXPathCompiler();
        try {
            xpc.declareNamespace(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX, MarkLogicQueryURI.EXT_NAMESPACE_URI);
            QName varConf = new QName("config");
            QName varUri = new QName("xqueryUri");
            QName varExtVars = new QName("extVars");
            xpc.declareVariable(varConf);
            xpc.declareVariable(varUri);
            xpc.declareVariable(varExtVars);
            XPathSelector xp = xpc.compile(MarkLogicQueryURI.EXT_NS_COMMON_PREFIX + ":" + MarkLogicQueryURI.FUNCTION_NAME + "($xqueryUri, $config, $extVars)").load();
            XdmValue xqConfig = XdmValue.wrap(CONNECT);
            xp.setVariable(varConf, xqConfig);
            String xqueryUri = this.getClass().getClassLoader().getResource("MarkLogicQueryURITest.xqy").toString();
            XdmAtomicValue xqUri = new XdmAtomicValue(xqueryUri);
            xp.setVariable(varUri, xqUri);
            xp.setVariable(varExtVars, XdmEmptySequence.getInstance());
            XdmValue result = xp.evaluate();
            SequenceIterator it = result.getUnderlyingValue().iterate();
            Item item = it.next();
            int count = 1;
            while (item != null) {
                assertEquals(Integer.toString(count++), item.getStringValue());
                item = it.next();
            }
            it.close();
        }
        catch (SaxonApiException | XPathException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Test OK with XSL
     * @throws XPathException
     * @throws TransformerConfigurationException
     * @throws URISyntaxException
     */
    @Test
    public void testXSL_QueryOK() throws XPathException, TransformerConfigurationException, URISyntaxException {
        TransformerFactory factory = TransformerFactory.newInstance();
        TransformerFactoryImpl tFactoryImpl = (TransformerFactoryImpl) factory;
        configuration.registerExtensionFunction(new MarkLogicQueryURI());
        tFactoryImpl.setConfiguration(configuration);
        try {
            Source xslt = new StreamSource(this.getClass().getClassLoader().getResource("MarkLogicQueryURITest_OK.xsl").toURI().toString());
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
     * Test KO with XSL - XQuery file not found
     * @throws XPathException
     * @throws TransformerConfigurationException
     * @throws URISyntaxException
     */
    @Test(expected = XPathException.class)
    public void testXSL_QueryKO_FileNotFound() throws XPathException, TransformerConfigurationException, URISyntaxException {
        TransformerFactory factory = TransformerFactory.newInstance();
        TransformerFactoryImpl tFactoryImpl = (TransformerFactoryImpl) factory;
        configuration.registerExtensionFunction(new MarkLogicQueryURI());
        tFactoryImpl.setConfiguration(configuration);
        try {
            Source xslt = new StreamSource(this.getClass().getClassLoader().getResource("MarkLogicQueryURITest_FileNotFound.xsl").toURI().toString());
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
     * @throws URISyntaxException
     * @throws SaxonApiException
     */
    @Test
    public void testXSL_ExternalVar_QueryOK() throws XPathException, TransformerConfigurationException, URISyntaxException, SaxonApiException {
        TransformerFactory factory = TransformerFactory.newInstance();
        TransformerFactoryImpl tFactoryImpl = (TransformerFactoryImpl) factory;
        configuration.registerExtensionFunction(new MarkLogicQueryURI());
        tFactoryImpl.setConfiguration(configuration);
        try {
            Source xslt = new StreamSource(this.getClass().getClassLoader().getResource("MarkLogicQueryURITest_ExternalVariables_OK.xsl").toURI().toString());
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
        catch (XPathException | TransformerConfigurationException | URISyntaxException | SaxonApiException ex) {
            System.err.println(ex.getMessage());
            throw ex;
        }
    }

}
