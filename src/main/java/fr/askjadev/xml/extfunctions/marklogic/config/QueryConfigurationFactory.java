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
package fr.askjadev.xml.extfunctions.marklogic.config;

import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.KeyValuePair;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.IntegerValue;

/**
 * Utility class QueryConfigurationFactory / Methods to instantiate a QueryConfiguration from the function call arguments
 * @author Axel Court
 */
public class QueryConfigurationFactory {
    
    public QueryConfiguration getConfig(Sequence[] args) throws XPathException {
        QueryConfiguration config = new QueryConfiguration();
        try {
            HashTrieMap configMap = (HashTrieMap) args[1].head();
            // Saxon 9.9 compatible code, use it once 9.8 support is dropped
            // Iterator<KeyValuePair> iterator = configMap.keyValuePairs().iterator();
            // while (iterator.hasNext()) {
            // KeyValuePair kv = iterator.next();
            AtomicIterator iterator = configMap.keys();
            AtomicValue key;
            while ((key = iterator.next()) != null) {
                KeyValuePair kv = configMap.getKeyValuePair(key);
                String k = kv.key.getStringValue();
                try {
                    switch (k) {
                        case "server":
                        case "user":
                        case "password":
                        case "database":
                        case "authentication":
                            config.set(k, kv.value.head().getStringValue());
                            break;
                        case "port":
                            config.set(k, ((IntegerValue) kv.value.head()).asBigInteger().intValue());
                            break;
                    }
                }
                catch (UnsupportedOperationException | ClassCastException ex) {
                    throw new XPathException("The value of the configuration property '" + k + "' is not a member of the expected datatype; see the type casting exception: " + ex.getMessage());
                }
            }
            if (config.getServer() == null ||
                config.getUser() == null ||
                config.getPassword() == null ||
                config.getPort() == null) {
                throw new XPathException("Some mandatory configuration values are missing. 'server', 'port', 'user' and 'password' must be specified.");
            }
            return config;
        }
        catch (ClassCastException ex) {
            throw new XPathException("The 2nd argument must be an XPath 3.0 map defining the server and query configuration.");
        }
    }
    
}
