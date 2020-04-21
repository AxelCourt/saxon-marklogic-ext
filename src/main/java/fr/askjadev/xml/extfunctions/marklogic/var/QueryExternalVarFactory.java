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
package fr.askjadev.xml.extfunctions.marklogic.var;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import javax.xml.bind.DatatypeConverter;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.ma.arrays.ArrayItem;
import net.sf.saxon.ma.arrays.ArrayItemType;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.KeyValuePair;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Base64BinaryValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.HexBinaryValue;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.QualifiedNameValue;

/**
 * Utility class QueryExternalVarFactory / Get the QueryExternalVar objects from the function call arguments
 * See test class EvalTest in MarkLogic Java client API for data conversion examples.
 * @author Axel Court
 */
public class QueryExternalVarFactory {
    
    private final Processor processor;
    private final XPathContext xpathContext;

    public QueryExternalVarFactory(Processor processor, XPathContext xpathContext) {
        this.processor = processor;
        this.xpathContext = xpathContext;
    }
    
    private final BuiltInAtomicType[] ATOMIC_AS_STRING = {
        BuiltInAtomicType.STRING,
        BuiltInAtomicType.NORMALIZED_STRING,
        BuiltInAtomicType.DATE,
        BuiltInAtomicType.DATE_TIME,
        BuiltInAtomicType.DATE_TIME_STAMP,
        BuiltInAtomicType.DAY_TIME_DURATION,
        BuiltInAtomicType.YEAR_MONTH_DURATION,
        BuiltInAtomicType.DURATION,
        BuiltInAtomicType.TIME,
        BuiltInAtomicType.G_DAY,
        BuiltInAtomicType.G_MONTH,
        BuiltInAtomicType.G_MONTH_DAY,
        BuiltInAtomicType.G_YEAR,
        BuiltInAtomicType.G_YEAR_MONTH,
        BuiltInAtomicType.DECIMAL,
        BuiltInAtomicType.ANY_URI,
        BuiltInAtomicType.QNAME
    };
    
    private final BuiltInAtomicType[] ATOMIC_AS_INT = {
        BuiltInAtomicType.INT,
        BuiltInAtomicType.INTEGER,
        BuiltInAtomicType.NEGATIVE_INTEGER,
        BuiltInAtomicType.NON_NEGATIVE_INTEGER,
        BuiltInAtomicType.NON_POSITIVE_INTEGER
    };
    
    private final BuiltInAtomicType[] ATOMIC_AS_DEC = {
        BuiltInAtomicType.DOUBLE,
        BuiltInAtomicType.FLOAT
    };
    
    private final BuiltInAtomicType[] ATOMIC_AS_BIN = {
        BuiltInAtomicType.BASE64_BINARY,
        BuiltInAtomicType.HEX_BINARY
    };
    
    private final BuiltInAtomicType[] ATOMIC_AS_BOOL = {
        BuiltInAtomicType.BOOLEAN
    };

    public ArrayList<QueryExternalVar> getExternalVariables(HashTrieMap variableMap) throws XPathException {
        ArrayList<QueryExternalVar> externalVariables = new ArrayList<>();
       	if (variableMap != null) {
            // Saxon 9.9 compatible code, use it once 9.8 support is dropped
            // Iterator<KeyValuePair> iterator = variableMap.keyValuePairs().iterator();
            // while (iterator.hasNext()) {
            // KeyValuePair kv = iterator.next();
            AtomicIterator iterator = variableMap.keys();
            AtomicValue key;
            while ((key = iterator.next()) != null) {
                KeyValuePair kv = variableMap.getKeyValuePair(key);
                try {
                    QualifiedNameValue variableQName = (QualifiedNameValue) kv.key;
                    QueryExternalVar variable = new QueryExternalVar(
                            variableQName.getNamespaceURI(),
                            variableQName.getPrefix(),
                            variableQName.getLocalName()
                    );
                    SequenceIterator sequenceValue = kv.value.iterate();
                    Item initialValue = sequenceValue.next();
                    if (sequenceValue.next() != null) {
                        throw new XPathException("The external variables supplied to the query can not be multivalued: see variable " + variableQName.getPrimitiveStringValue() + ".");
                    }
                    sequenceValue.close();
                    variable.setSaxonValue(initialValue);
                    if (initialValue != null) {
                        // Cast the value in a MarkLogic Server compatible format
                        try {
                            variable.setValue(getVariableValue(initialValue));
                        }
                        catch (XPathException ex) {
                            throw new XPathException("Error while trying to cast external variable " + variableQName.getPrimitiveStringValue() + " : " + ex.getMessage());
                        }
                    }
                    // In case of an empty-sequence()
                    else {
                        variable.setValue(null);
                    }
                    externalVariables.add(variable);
                }
                catch (ClassCastException ex) {
                    throw new XPathException("The external variables map keys must be of type xs:QName.");
                }
            }
        }
        return externalVariables;
    }
      
    private Object getVariableValue(Item initialValue) throws XPathException {
        Object value = null;
        TypeHierarchy th = xpathContext.getConfiguration().getTypeHierarchy();
        ItemType itemType = Type.getItemType(initialValue, th);
        Boolean isMapType = MapType.ANY_MAP_TYPE.matches(initialValue, th);
        Boolean isArrayType = ArrayItemType.ANY_ARRAY_TYPE.matches(initialValue, th);
        // Atomic value
        if (itemType.isAtomicType()) {
            value = castAtomicValue(initialValue, itemType);
        }
        // Node
        if (Type.isNodeType(itemType)) {
            value = castNodeValue(initialValue, itemType);
        }
        // Map
        if (isMapType) {
            value = castMapValue(initialValue, itemType);
        }
        // Array
        if (isArrayType) {
            value = castArrayValue(initialValue, itemType);
        }
        if (value == null) {
            throw new XPathException("Incompatible type: " + itemType.toString()); 
        }
        return value;
    }
    
    private Object castAtomicValue(Item initialValue, ItemType initialValueType) throws XPathException {
        for (BuiltInAtomicType atomicAsStringType : ATOMIC_AS_STRING) {
            if (atomicAsStringType.equals(initialValueType)) {
                return initialValue.getStringValue();
            }
        }
        for (BuiltInAtomicType atomicAsIntType : ATOMIC_AS_INT) {
            if (atomicAsIntType.equals(initialValueType)) {
                IntegerValue intValue = (IntegerValue) initialValue.atomize();
                return intValue.asBigInteger().intValue();
            }
        }
        for (BuiltInAtomicType atomicAsNbType : ATOMIC_AS_DEC) {
            if (atomicAsNbType.equals(initialValueType)) {
                NumericValue decValue = (NumericValue) initialValue.atomize();
                return decValue.getDecimalValue();
            }
        }
        for (BuiltInAtomicType atomicAsBoolType : ATOMIC_AS_BOOL) {
            if (atomicAsBoolType.equals(initialValueType)) {
                BooleanValue boolValue = (BooleanValue) initialValue.atomize();
                return boolValue.effectiveBooleanValue();
            }
        }
        for (BuiltInAtomicType atomicAsBinType : ATOMIC_AS_BIN) {
            if (atomicAsBinType.equals(initialValueType)) {
                if (initialValueType.equals(BuiltInAtomicType.BASE64_BINARY)) {
                    Base64BinaryValue base64Value = (Base64BinaryValue) initialValue.atomize();
                    return DatatypeConverter.printBase64Binary(base64Value.getBinaryValue());
                }
                else if (initialValueType.equals(BuiltInAtomicType.HEX_BINARY)) {
                    HexBinaryValue hexValue = (HexBinaryValue) initialValue.atomize();
                    return DatatypeConverter.printHexBinary(hexValue.getBinaryValue());
                }
            }
        }
        return null;
    }
    
    private Object castNodeValue(Item initialValue, ItemType initialValueType) throws XPathException {
        NodeInfo nodeValue = (NodeInfo) initialValue;
        XdmNode node = new XdmNode(nodeValue);
        if (nodeValue.getNodeKind() == Type.ELEMENT                 ||
            nodeValue.getNodeKind() == Type.DOCUMENT                ||
            nodeValue.getNodeKind() == Type.COMMENT                 ||
            nodeValue.getNodeKind() == Type.PROCESSING_INSTRUCTION) {
            // Basic string serialization
            return node.toString();
        }
        // Wrap an attribute node around a dummy element
        if (nodeValue.getNodeKind() == Type.ATTRIBUTE) {
            return "<mkl-ext:dummy-element xmlns:mkl-ext=" + '"' + "fr:askjadev:xml:extfunctions" + '"' + " " + node.toString() + "/>";
        }
        if (nodeValue.getNodeKind() == Type.TEXT) {
            return new StringHandle(node.toString()).withFormat(Format.TEXT);
        }
        return null;
    }

    private Object castMapValue(Item initialValue, ItemType itemType) throws XPathException {
        // Good for simple values, probably not for node values...
        try {
            XdmValue mapValue = XdmValue.wrap((MapItem) initialValue);
            StringWriter jsonString = new StringWriter();
            Serializer jsonSerializer = processor.newSerializer();
            jsonSerializer.setOutputWriter(jsonString);
            jsonSerializer.setOutputProperty(Serializer.Property.METHOD, "json");
            jsonSerializer.serializeXdmValue(mapValue);
            ObjectNode jsonNode = (ObjectNode) new ObjectMapper().readTree(jsonString.toString());
            jsonSerializer.close();
            jsonString.close();
            return jsonNode;
        }
        catch (IOException | SaxonApiException ex) {
            throw new  XPathException("Error when trying to transform the map value into a JSON ObjectNode: " + ex.getMessage());
        }
    }

    private Object castArrayValue(Item initialValue, ItemType itemType) throws XPathException {
        // Good for simple values, probably not for node values...
        try {
            XdmValue arrayValue = XdmValue.wrap((ArrayItem) initialValue);
            StringWriter jsonString = new StringWriter();
            Serializer jsonSerializer = processor.newSerializer();
            jsonSerializer.setOutputWriter(jsonString);
            jsonSerializer.setOutputProperty(Serializer.Property.METHOD, "json");
            jsonSerializer.serializeXdmValue(arrayValue);
            ArrayNode arrayNode = (ArrayNode) new ObjectMapper().readTree(jsonString.toString());
            jsonSerializer.close();
            jsonString.close();
            return arrayNode;
        }
        catch (IOException | SaxonApiException ex) {
            throw new  XPathException("Error when trying to transform the array value into a JSON ArrayNode: " + ex.getMessage());
        }
    }
    
}
