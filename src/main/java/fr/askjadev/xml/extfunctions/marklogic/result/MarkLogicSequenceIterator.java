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
package fr.askjadev.xml.extfunctions.marklogic.result;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import fr.askjadev.xml.extfunctions.marklogic.AbstractMLExtensionFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

/**
 * Utility class MarkLogicSequenceIterator / Query result iterator
 *
 * @author Axel Court
 */
public class MarkLogicSequenceIterator implements SequenceIterator, AutoCloseable {

    private final EvalResultIterator result;
    private final DocumentBuilder builder;
    private final XPathContext xpathContext;
    private final DatabaseClient session;
    private Integer resultCount;
    private boolean closed = false;

    public MarkLogicSequenceIterator(EvalResultIterator result, DocumentBuilder builder, XPathContext xpc) {
        super();
        this.result = result;
        this.builder = builder;
        this.xpathContext = xpc;
        this.session = null;
        this.resultCount = 0;
    }

    public MarkLogicSequenceIterator(EvalResultIterator result, DocumentBuilder builder, XPathContext xpc, DatabaseClient session) {
        super();
        this.result = result;
        this.builder = builder;
        this.xpathContext = xpc;
        this.session = session;
        this.resultCount = 0;
    }

    @Override
    public Item next() throws XPathException {
        if (result.hasNext()) {
            resultCount++;
            EvalResult currentResult = result.next();
            // Logger.getLogger(AbstractMLExtensionFunction.class.getName()).log(Level.INFO, currentResult.getType().toString());
            XdmValue xdmValue = EvalResultConverter.convertToXdmValue(currentResult, builder, xpathContext);
            if (xdmValue == null) {
                throw new XPathException("One of the query results could not be converted to a Saxon XdmValue because its type is not supported: " + currentResult.getType());
            }
            // Logger.getLogger(AbstractMLExtensionFunction.class.getName()).log(Level.INFO, xdmValue.toString());
            Item item = xdmValue.getUnderlyingValue().head();
            // Logger.getLogger(AbstractMLExtensionFunction.class.getName()).log(Level.INFO, Type.displayTypeName(item));
            return item;
        }
        else {
            close();
            return null;
        }
    }

    @Override
    public void close() {
        // Logger.getLogger(AbstractMLExtensionFunction.class.getName()).log(Level.INFO, "Total result(s): {0}", resultCount);
        if (closed) {
            return;
        }
        try {
            // Logger.getLogger(AbstractMLExtensionFunction.class.getName()).log(Level.INFO, "Closing sequence iterator.");
            closed = true;
            result.close();
            if (session != null) {
                session.release();
            }
        }
        catch (Exception ex) {
            Logger.getLogger(AbstractMLExtensionFunction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public int getProperties() {
        return 0;
    }

}
