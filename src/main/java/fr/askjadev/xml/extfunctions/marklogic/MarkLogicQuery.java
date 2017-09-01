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
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.ForbiddenUserException;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.LazySequence;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.tiny.TinyElementImpl;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

/**
 * This class is an extension function for Saxon. It must be declared by
 * <tt>configuration.registerExtensionFunction(new MarkLogicQuery());</tt>, or
 * via Saxon Configuration file
 * (<a href=http://www.saxonica.com/documentation9.7/index.html#!configuration/configuration-file>Saxon
 * documentation</a>). In gaulois-pipe, it just has to be in the classpath.
 *
 * Use as :
 * <tt>declare namespace els-ext = 'fr:askjadev:xml:extfunctions';
 * mkl-ext:marklogic-query("for $i in 1 to 10 return
 * &lt;test&gt;{$i}&lt;/test&gt;", &lt;marklogic&gt;
 * &lt;server&gt;localhost&lt;/server&gt; &lt;port&gt;1984&lt;/port&gt;
 * &lt;user&gt;user&lt;/user&gt; &lt;password&gt;password&lt;/password&gt;
 * &lt;/marklogic&gt; );</tt>
 * Or :
 * <tt>declare namespace els-ext = 'fr:askjadev:xml:extfunctions';
 * mkl-ext:marklogic-query("for $i in 1 to 10 return
 * &lt;test&gt;{$i}&lt;/test&gt;",
 * "localhost", "1984", "user", "password");</tt>
 *
 * Many thanks to Christophe Marchand for the base code!
 * 
 * @author Axel Court
 */
public class MarkLogicQuery extends ExtensionFunctionDefinition {

    public static final String EXT_NAMESPACE_URI = "fr:askjadev:xml:extfunctions";
    public static final String FUNCTION_NAME = "marklogic-query";
    public static final String EXT_NS_COMMON_PREFIX = "mkl-ext";

    @Override
    public StructuredQName getFunctionQName() {
        return new StructuredQName(EXT_NS_COMMON_PREFIX, EXT_NAMESPACE_URI, FUNCTION_NAME);
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{
            SequenceType.SINGLE_STRING,
            SequenceType.SINGLE_ITEM,
            SequenceType.OPTIONAL_STRING,
            SequenceType.OPTIONAL_STRING,
            SequenceType.OPTIONAL_STRING};
    }

    @Override
    public int getMinimumNumberOfArguments() {
        return 2;
    }

    @Override
    public int getMaximumNumberOfArguments() {
        return 5;
    }

    @Override
    public SequenceType getResultType(net.sf.saxon.value.SequenceType[] sts) {
        return SequenceType.ANY_SEQUENCE;
    }

    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {
            @Override
            public Sequence call(XPathContext xpc, Sequence[] sqncs) throws XPathException {
                String[] args = checkArgs(xpc, sqncs);
                String xquery = args[0];
                String server = args[1];
                int port = Integer.parseInt(args[2]);
                String user = args[3];
                String password = args[4];
                Processor proc = new Processor(xpc.getConfiguration());
                try {
                    DatabaseClient session = DatabaseClientFactory.newClient(server, port, new DatabaseClientFactory.DigestAuthContext(user, password));
                    ServerEvaluationCall call = session.newServerEval();
                    call.xquery(xquery);
                    EvalResultIterator result = call.eval();
                    DocumentBuilder builder = proc.newDocumentBuilder();
                    MarkLogicSequenceIterator it = new MarkLogicSequenceIterator(result, builder, session);
                    return new LazySequence(it);
                } catch (FailedRequestException | ForbiddenUserException ex) {
                    throw new XPathException(ex);
                }
            }
            private String[] checkArgs(XPathContext xpc, Sequence[] sqncs) throws XPathException {
                switch (sqncs.length) {
                    case 2:
                        String server = null, port = null, user = null, password = null;
                        try {
                            TinyElementImpl basexNode = ((TinyElementImpl) sqncs[1].head());
                            AxisIterator iterator = basexNode.iterateAxis(AxisInfo.CHILD);
                            for (NodeInfo ni = iterator.next(); ni != null; ni = iterator.next()) {
                                if (ni.getNodeKind() == Type.ELEMENT) {
                                    switch (ni.getLocalPart()) {
                                        case "server":
                                            server = ni.getStringValue();
                                            break;
                                        case "port":
                                            port = ni.getStringValue();
                                            break;
                                        case "user":
                                            user = ni.getStringValue();
                                            break;
                                        case "password":
                                            password = ni.getStringValue();
                                            break;
                                        default:
                                            throw new XPathException("Children elements of 'marklogic' must be 'server', 'port', 'user' and 'password'.");
                                    }
                                }
                            }
                            return new String[]{
                                ((StringValue) sqncs[0].head()).getStringValue(),
                                server,
                                port,
                                user,
                                password
                            };
                        } catch (ClassCastException ex) {
                            throw new XPathException("When using the 2 parameters signature, the second parameter must be of type: element(marklogic).");
                        }
                    case 5:
                        try {
                            return new String[]{
                                ((StringValue) sqncs[0].head()).getStringValue(),
                                ((StringValue) sqncs[1].head()).getStringValue(),
                                ((StringValue) sqncs[2].head()).getStringValue(),
                                ((StringValue) sqncs[3].head()).getStringValue(),
                                ((StringValue) sqncs[4].head()).getStringValue()
                            };
                        } catch (ClassCastException ex) {
                            throw new XPathException("When using the 5 parameters signature, all parameters must be of type: xs:string.");
                        }
                    default:
                        throw new XPathException("Illegal number of arguments. "
                                + "Arguments are either (xs:string, element(marklogic)), "
                                + "or (xs:string, xs:string, xs:string, xs:string, xs:string).");
                }
            }
        };
    }
    
    protected class MarkLogicSequenceIterator implements SequenceIterator, AutoCloseable {
        private final EvalResultIterator result;
        private final DocumentBuilder builder;
        private final DatabaseClient session;
        private boolean closed = false;
        
        public MarkLogicSequenceIterator(EvalResultIterator result, DocumentBuilder builder, DatabaseClient session) {
            super();
            this.result = result;
            this.builder = builder;
            this.session = session;
        }

        @Override
        public Item next() throws XPathException {
            try {
                if(result.hasNext()) {
                    StreamSource source = new StreamSource(new ByteArrayInputStream(result.next().getString().getBytes("UTF8")));
                    XdmNode node = builder.build(source);
                    return node.getUnderlyingNode();
                } else {
                    close();
                    return null;
                }
            } catch(IOException | SaxonApiException ex) {
                throw new XPathException(ex);
            }
        }

        @Override
        public void close() {
            if(closed) return;
            try {
                // Logger.getLogger(MarkLogicQuery.class.getName()).log(Level.INFO, "Closing sequence iterator.");
                closed = true;
                result.close();
                session.release();
            } catch (Exception ex) {
                Logger.getLogger(MarkLogicQuery.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public SequenceIterator getAnother() throws XPathException {
            return null;
        }

        @Override
        public int getProperties() {
            return 0;
        }   
    }

}