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
package fr.askjadev.xml.extfunctions.marklogic.utils;

import java.util.ArrayList;
import java.util.Iterator;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.SecurityContext;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import fr.askjadev.xml.extfunctions.marklogic.config.QueryConfiguration;
import fr.askjadev.xml.extfunctions.marklogic.result.MarkLogicSequenceIterator;
import fr.askjadev.xml.extfunctions.marklogic.var.QueryExternalVar;
import fr.askjadev.xml.extfunctions.marklogic.var.QueryExternalVarFactory;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.om.LazySequence;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.trans.XPathException;

/**
 * Utility methods / Database manipulation
 *
 * @author Axel Court
 */
public class DatabaseUtils {

    public static DatabaseClient createMarkLogicClient(QueryConfiguration config) {
        DatabaseClientFactory.SecurityContext authContext;
        switch (config.getAuthentication()) {
            case "digest":
                authContext = new DatabaseClientFactory.DigestAuthContext(config.getUser(), config.getPassword());
                break;
            default:
                authContext = new DatabaseClientFactory.BasicAuthContext(config.getUser(), config.getPassword());
        }
        // Init session
        return makeNewClient(config.getServer(), config.getPort(), config.getDatabase(), authContext);
    }

    public static DatabaseClient makeNewClient(String server, Integer port, String database, SecurityContext authContext) {
        if (!(database == null)) {
            return DatabaseClientFactory.newClient(server, port, database, authContext);
        }
        else {
            return DatabaseClientFactory.newClient(server, port, authContext);
        }
    }

    public static ServerEvaluationCall addExternalVariables(ServerEvaluationCall call, XPathContext xpc, Processor proc, HashTrieMap variableMap) throws XPathException {
        QueryExternalVarFactory varFactory = new QueryExternalVarFactory(proc, xpc);
        ArrayList<QueryExternalVar> externalVars = varFactory.getExternalVariables(variableMap);
        Iterator<QueryExternalVar> it = externalVars.iterator();
        while (it.hasNext()) {
            QueryExternalVar var = it.next();
            call = var.addToCall(call);
        }
        return call;
    }

    public static LazySequence getQueryResult(ServerEvaluationCall call, DocumentBuilder builder, XPathContext xpc) {
        EvalResultIterator result = call.eval();
        MarkLogicSequenceIterator it = new MarkLogicSequenceIterator(result, builder, xpc);
        return new LazySequence(it);
    }

}
