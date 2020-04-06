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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.http.client.utils.URIUtils;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.InputStreamHandle;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.StandardUnparsedTextResolver;
import net.sf.saxon.trans.XPathException;

/**
 * Utility methods / XQuery input
 *
 * @author Axel Court
 */
public class XQueryUtils {

    public static InputStreamHandle getXQueryFromURI(XPathContext xpc, String queryUri, String staticBaseUri) throws XPathException {
        try {
            // Logger.getLogger(AbstractMLExtensionFunction.class.getName()).log(Level.INFO, queryUri);
            // Resolve the XQuery URI if it is relative
            URI queryUriResolved = URIUtils.resolve(new URI(staticBaseUri), queryUri);
            // Logger.getLogger(AbstractMLExtensionFunction.class.getName()).log(Level.INFO, queryUriResolved.toString());
            // Try to detect early when the URL points to nothing -> otherwise there can be a NullPointerException raised by StandardUnparsedTextResolver.connect()
            if (queryUriResolved.toURL().openConnection().getInputStream() == null) {
                throw new IOException("File not found: " + queryUriResolved.toString());
            }
            // Get the XQuery content as unparsed text using Saxon convenient StandardUnparsedTextResolver class (Reader -> InputStream -> InputStreamHandle)
            StandardUnparsedTextResolver unparsedTextResolver = new StandardUnparsedTextResolver();
            InputStreamHandle xquery = new InputStreamHandle(new ReaderInputStream(unparsedTextResolver.resolve(queryUriResolved, "UTF-8", xpc.getConfiguration()), "UTF-8"));
            xquery.setFormat(Format.TEXT);
            return xquery;
        }
        catch (URISyntaxException | IOException | XPathException ex) {
            throw new XPathException("Error while trying to load the XQuery file: " + queryUri + "; see: " + ex.getMessage());
        }
    }

}
