/*
 * The MIT License
 *
 * Copyright 2018 Axel Court.
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

import fr.askjadev.xml.extfunctions.marklogic.utils.DatabaseUtils;
import fr.askjadev.xml.extfunctions.marklogic.utils.XQueryUtils;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.ForbiddenUserException;
import com.marklogic.client.MarkLogicIOException;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.InputStreamHandle;
import fr.askjadev.xml.extfunctions.marklogic.config.QueryConfiguration;
import fr.askjadev.xml.extfunctions.marklogic.config.QueryConfigurationFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.*;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

/**
 * Abstract class representing a Saxon MarkLogic extension function
 * @author Axel Court + Emmanuel Tourdot
 */
public abstract class AbstractMLExtensionFunction extends ExtensionFunctionDefinition {
    
    private String staticBaseUri;
    
    enum ExtentionType {
        XQUERY_URI, XQUERY_STRING, MODULE;
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[] {
            SequenceType.SINGLE_STRING,
            MapType.OPTIONAL_MAP_ITEM,
            MapType.OPTIONAL_MAP_ITEM
        };
    }

    @Override
    public int getMinimumNumberOfArguments() {
        return 2;
    }

    @Override
    public int getMaximumNumberOfArguments() {
        return 3;
    }

    @Override
    public SequenceType getResultType(net.sf.saxon.value.SequenceType[] sts) {
        return SequenceType.ANY_SEQUENCE;
    }

    ExtensionFunctionCall constructExtensionFunctionCall(final ExtentionType type) {

        return new ExtensionFunctionCall() {
            
            @Override
            public Sequence call(XPathContext xpc, Sequence[] args) throws XPathException {
                // Check and get the configuration
                QueryConfiguration config = new QueryConfigurationFactory().getConfig(args);
                // Get the XQuery or the module to invoke
                String moduleOrQuery = getXQueryOrModule(args);
                // Launch
                Processor proc = new Processor(xpc.getConfiguration());
                DatabaseClient session = null;
                try {
                    session = DatabaseUtils.createMarkLogicClient(config);
                    // Eval query and get result
                    DocumentBuilder builder = proc.newDocumentBuilder();
                    ServerEvaluationCall call = session.newServerEval();
                    switch (type) {
                        case MODULE:
                            call.modulePath(moduleOrQuery);
                            break;
                        case XQUERY_STRING:
                            call.xquery(moduleOrQuery);
                            break;
                        case XQUERY_URI:
                            // Read the XQuery and send it as an InputStreamHandle
                            InputStreamHandle xquery = XQueryUtils.getXQueryFromURI(xpc, moduleOrQuery, staticBaseUri);
                            call.xquery(xquery);
                            xquery.close();
                            break;
                    }
                    if (args.length == 3) {
                    	try {
                            call = DatabaseUtils.addExternalVariables(call, xpc, proc, (HashTrieMap) args[2].head());
                    	}
                    	catch (ClassCastException ex) {
                            throw new XPathException("The 3d argument must be an XPath 3.0 map defining the query external variables.");
                        }
                    }
                    return DatabaseUtils.getQueryResult(call, builder, xpc);
                }
                catch (FailedRequestException | ForbiddenUserException | MarkLogicIOException ex) {
                    throw new XPathException(ex);
                }
            }
            
            @Override
            public void supplyStaticContext(StaticContext context, int locationId, Expression[] args) throws XPathException {
                // Add information about the static context
                staticBaseUri = context.getStaticBaseURI();
            }
            
        };
    
    }
    
    private String getXQueryOrModule(Sequence [] args) throws XPathException {
        // May be a reference to a module, the URI to a local XQuery or a string containing the XQuery
        String xqueryOrModule = ((StringValue) args[0].head()).getStringValue();
        return xqueryOrModule;
    }

}
