/*
 * The MIT License
 *
 * Copyright 2018 ext-acourt.
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

import com.marklogic.client.eval.EvalResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.ma.json.JsonHandlerMap;
import net.sf.saxon.ma.json.JsonParser;
import net.sf.saxon.om.Item;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import org.apache.commons.io.IOUtils;

/**
 * Utility class EvalResultConverter / Conversion methods from query EvalResult to XdmValue
 * @author Axel Court
 */
public class EvalResultConverter {
    
    private static final EvalResult.Type[] NODE_TYPES = {
        EvalResult.Type.COMMENT,
        EvalResult.Type.PROCESSINGINSTRUCTION,
        EvalResult.Type.TEXTNODE,
        EvalResult.Type.XML
    };

    public static XdmValue convertToXdmValue(EvalResult evalResult, DocumentBuilder builder, XPathContext xpc) throws XPathException {
        try {
            // Atomic values
            if (evalResult.getType().equals(EvalResult.Type.ANYURI)) {
                return new XdmAtomicValue(new URI(evalResult.getString()));
            }
            if (evalResult.getType().equals(EvalResult.Type.BASE64BINARY)) {
                return new XdmAtomicValue(evalResult.getString(), ItemType.BASE64_BINARY);
            }
            if (evalResult.getType().equals(EvalResult.Type.BOOLEAN)) {
                return new XdmAtomicValue(evalResult.getBoolean());
            }
            if (evalResult.getType().equals(EvalResult.Type.DATE)) {
                return new XdmAtomicValue(evalResult.getString(), ItemType.DATE);
            }
            if (evalResult.getType().equals(EvalResult.Type.DATETIME)) {
                return new XdmAtomicValue(evalResult.getString(), ItemType.DATE_TIME);
            }
            if (evalResult.getType().equals(EvalResult.Type.DECIMAL)) {
                return new XdmAtomicValue(BigDecimal.valueOf(evalResult.getNumber().doubleValue()));
            }
            if (evalResult.getType().equals(EvalResult.Type.DOUBLE)) {
                return new XdmAtomicValue(evalResult.getNumber().doubleValue());
            }
            if (evalResult.getType().equals(EvalResult.Type.DURATION)) {
                return new XdmAtomicValue(evalResult.getString(), ItemType.DURATION);
            }
            if (evalResult.getType().equals(EvalResult.Type.FLOAT)) {
                return new XdmAtomicValue(evalResult.getNumber().floatValue());
            }
            if (evalResult.getType().equals(EvalResult.Type.GDAY)) {
                return new XdmAtomicValue(evalResult.getString(), ItemType.G_DAY);
            }
            if (evalResult.getType().equals(EvalResult.Type.GMONTH)) {
                return new XdmAtomicValue(evalResult.getString(), ItemType.G_MONTH);
            }
            if (evalResult.getType().equals(EvalResult.Type.GMONTHDAY)) {
                return new XdmAtomicValue(evalResult.getString(), ItemType.G_MONTH_DAY);
            }
            if (evalResult.getType().equals(EvalResult.Type.GYEAR)) {
                return new XdmAtomicValue(evalResult.getString(), ItemType.G_YEAR);
            }
            if (evalResult.getType().equals(EvalResult.Type.GYEARMONTH)) {
                return new XdmAtomicValue(evalResult.getString(), ItemType.G_YEAR_MONTH);
            }
            if (evalResult.getType().equals(EvalResult.Type.HEXBINARY)) {
                return new XdmAtomicValue(evalResult.getString(), ItemType.HEX_BINARY);
            }
            if (evalResult.getType().equals(EvalResult.Type.INTEGER)) {
                return new XdmAtomicValue(evalResult.getNumber().intValue());
            }
            if (evalResult.getType().equals(EvalResult.Type.QNAME)) {
                // FIXME: This is wrong! How to get the NS URI associated with the QName?
                return new XdmAtomicValue(new QName("", evalResult.getString()));
            }
            if (evalResult.getType().equals(EvalResult.Type.STRING)) {
                return new XdmAtomicValue(evalResult.getString());
            }
            if (evalResult.getType().equals(EvalResult.Type.TIME)) {
                return new XdmAtomicValue(evalResult.getString(), ItemType.TIME);
            }
            // Nodes
            for (EvalResult.Type nodeType : NODE_TYPES) {
                if (evalResult.getType().equals(nodeType)) {
                    XdmNode xdmEvalResultNode;
                    String evalResultNodeAsString = evalResult.getString();
                    // evalResultNodeAsString is already a document-node()
                    // document-node() serialization of EvalResult always starts with the XML declaration "<?xml...?>"
                    // FIXME: There must be better ways to know if an EvalResult.Type "XML" is a document-node()...
                    if (evalResultNodeAsString.startsWith("<?xml")) {
                        StreamSource source = new StreamSource(IOUtils.toInputStream(evalResultNodeAsString, "UTF-8"));
                        xdmEvalResultNode = (XdmNode) builder.build(source);
                    }
                    // evalResultNodeAsString is not a document-node(), so let's wrap it with a fake root element
                    else {
                        StreamSource source = new StreamSource(IOUtils.toInputStream("<dummy-wrapper>" + evalResultNodeAsString + "</dummy-wrapper>", "UTF-8"));
                        XdmNode xdmDoc = (XdmNode) builder.build(source);
                        XdmNode xdmRootElt = (XdmNode) xdmDoc.axisIterator(Axis.CHILD).next();
                        xdmEvalResultNode = (XdmNode) xdmRootElt.axisIterator(Axis.CHILD).next();
                    }
                    return xdmEvalResultNode;
                }
            }
            // Maps and arrays
            // Good for simple values, probably not for node values...
            if (evalResult.getType().equals(EvalResult.Type.JSON)) {
                JsonHandlerMap jsonHandler = new JsonHandlerMap(xpc, JsonParser.LIBERAL);
                JsonParser jsonParser = new JsonParser();
                jsonParser.parse(evalResult.getString(), JsonParser.LIBERAL, jsonHandler, xpc);
                Item mapOrArrayItem = jsonHandler.getResult().head();
                XdmValue mapOrArrayXdmValue = new XdmValue(mapOrArrayItem){};
                return mapOrArrayXdmValue;
            }
            // MarkLogic explicit null-node() -> sent as an empty-sequence() to Saxon 
            // If the XQuery result is an actual empty-sequence(), nothing is returned
            if (evalResult.getType().equals(EvalResult.Type.NULL)) {
                return new XdmValue(EmptySequence.getInstance().asItem()){};
            }
            return null;
        }
        catch (URISyntaxException | SaxonApiException | IOException ex) {
            throw new XPathException("Error while trying to cast (one of) the query result(s): " + ex.getMessage());
        }
    }
    
}