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
package fr.askjadev.xml.extfunctions.marklogic.reflexive;

import java.io.IOException;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.InputStreamHandle;
import fr.askjadev.xml.extfunctions.marklogic.utils.DatabaseUtils;
import fr.askjadev.xml.extfunctions.marklogic.utils.XQueryUtils;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.om.LazySequence;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.trans.XPathException;

/**
 * Reflexive extension function to establish a connection and send queries to a
 * MarkLogic instance. It is a simplification of the main integrated extension
 * functions.
 *
 * @author Marc Messéant
 */
public class SimpleMarkLogicConnection {

    private Connection dataBaseConnection;

    public SimpleMarkLogicConnection(String host, String port, String username, String password) {
        dataBaseConnection = new Connection(host, port, username, password);
    }

    public Connection getConnection() {
        return dataBaseConnection;
    }

    // Read the XQuery code from a file and execute it
    static public LazySequence markLogicQueryUri(XPathContext xpc, SimpleMarkLogicConnection connection, String uri, String staticBaseUri) throws IOException, XPathException {
        return markLogicQueryUri(xpc, connection, uri, staticBaseUri, null);
    }

    static public LazySequence markLogicQueryUri(XPathContext xpc, SimpleMarkLogicConnection connection, String uri, String staticBaseUri, Sequence[] extVars) throws IOException, XPathException {
        Processor proc = new Processor(xpc.getConfiguration());
        DocumentBuilder builder = proc.newDocumentBuilder();
        InputStreamHandle xquery = XQueryUtils.getXQueryFromURI(xpc, uri, staticBaseUri);
        ServerEvaluationCall call = getServerEvaluationCall(xpc, proc, connection, staticBaseUri, extVars);
        call.xquery(xquery);
        xquery.close();
        return DatabaseUtils.getQueryResult(call, builder, xpc);
    }

    // The XQuery content is directly supplied
    static public LazySequence markLogicQuery(XPathContext xpc, SimpleMarkLogicConnection connection, String xquery, String staticBaseUri) throws IOException, XPathException {
        return markLogicQuery(xpc, connection, xquery, staticBaseUri, null);
    }

    static public LazySequence markLogicQuery(XPathContext xpc, SimpleMarkLogicConnection connection, String xquery, String staticBaseUri, Sequence[] extVars) throws IOException, XPathException {
        Processor proc = new Processor(xpc.getConfiguration());
        DocumentBuilder builder = proc.newDocumentBuilder();
        ServerEvaluationCall call = getServerEvaluationCall(xpc, proc, connection, staticBaseUri, extVars);
        call.xquery(xquery);
        return DatabaseUtils.getQueryResult(call, builder, xpc);
    }

    // Invoke an XQuery module
    static public LazySequence markLogicQueryInvoke(XPathContext xpc, SimpleMarkLogicConnection connection, String path, String staticBaseUri) throws IOException, XPathException {
        return markLogicQueryInvoke(xpc, connection, path, staticBaseUri, null);
    }

    static public LazySequence markLogicQueryInvoke(XPathContext xpc, SimpleMarkLogicConnection connection, String path, String staticBaseUri, Sequence[] extVars) throws IOException, XPathException {
        Processor proc = new Processor(xpc.getConfiguration());
        DocumentBuilder builder = proc.newDocumentBuilder();
        ServerEvaluationCall call = getServerEvaluationCall(xpc, proc, connection, staticBaseUri, extVars);
        call.modulePath(path);
        return DatabaseUtils.getQueryResult(call, builder, xpc);
    }

    private static ServerEvaluationCall getServerEvaluationCall(XPathContext xpc, Processor proc, SimpleMarkLogicConnection connection, String staticBaseUri, Sequence[] extVars) throws XPathException {
        DatabaseClient session = connection.getConnection().getDatabaseClient();
        ServerEvaluationCall call = session.newServerEval();
        if (extVars != null) {
            call = DatabaseUtils.addExternalVariables(call, xpc, proc, (HashTrieMap) extVars[0].head());
        }
        return call;
    }

    static public void releaseConnection(XPathContext xpc, SimpleMarkLogicConnection connection) {
        DatabaseClient session = connection.getConnection().getDatabaseClient();
        session.release();
    }

    private class Connection {

        private DatabaseClient client;

        public Connection(String host, String port, String username, String password) {
            client = DatabaseUtils.makeNewClient(host, Integer.parseInt(port), null, new DatabaseClientFactory.DigestAuthContext(username, password));
        }

        public DatabaseClient getDatabaseClient() {
            return client;
        }

    }

}
