# saxon-marklogic-ext


Extension functions for Saxon allowing XQueries to be sent to MarkLogic Server.


The extensions may be registered:
* in a Java program, by calling Saxon method <tt>registerExtensionFunction()</tt> and supplying the extension classes - for instance: <tt>configuration.registerExtensionFunction(new MarkLogicQuery());</tt>
* through a Saxon Configuration file (<a href=http://www.saxonica.com/documentation/index.html#!configuration/configuration-file>Saxon documentation</a>).


They are also <a href=https://github.com/cmarchand/gaulois-pipe>gaulois-pipe</a> services. The jar just has to be in the classpath for the functions to be used with gaulois-pipe.


The project is officially built around *Saxon 9* (HE/PE/EE) from <a href=http://www.saxonica.com>Saxonica</a> and XPath / XSLT 3.0.

Starting with version **1.3.0**, both *Saxon 9.9* and *Saxon 9.8* are supported.

Starting with version **1.1.0**, *Saxon 9.7* is no longer supported (last compatible release: **1.0.7-97**).


## Usage


### Send the XQuery as a string

Java class: <tt>fr.askjadev.xml.extfunctions.marklogic.MarkLogicQuery</tt>

<pre>&lt;xsl:sequence xmlns:mkl-ext="fr:askjadev:xml:extfunctions" select="
  mkl-ext:marklogic-query(
    'for $i in 1 to 10 return {$i}',
    map{
      'server':'localhost',
      'port':8004,
      'user':'admin',
      'password':'admin'
    }
  )"
/&gt;</pre>


### Invoke an XQuery module already deployed on MarkLogic Server

Java class: <tt>fr.askjadev.xml.extfunctions.marklogic.MarkLogicQueryInvoke</tt>

<pre>&lt;xsl:sequence xmlns:mkl-ext="fr:askjadev:xml:extfunctions" select="
  mkl-ext:marklogic-query-invoke(
    '/path/to/module.xqy',
    map{
      'server':'localhost',
      'port':8004,
      'user':'admin',
      'password':'admin'
    }
  )"
/&gt;</pre>


### Read an XQuery file from an URI and run it

Java class: <tt>fr.askjadev.xml.extfunctions.marklogic.MarkLogicQueryInvoke</tt>

<pre>&lt;xsl:sequence xmlns:mkl-ext="fr:askjadev:xml:extfunctions" select="
  mkl-ext:marklogic-query-uri(
    'file:/path/to/file.xqy',
    map{
      'server':'localhost',
      'port':8004,
      'user':'admin',
      'password':'admin'
    }
  )"
/&gt;</pre>


### Additional information

The second parameter is an XPath 3.0 map containing the server and database configuration.

The options "server" (<tt>xs:string</tt>), "port" (<tt>xs:integer</tt>), "user" (<tt>xs:string</tt>) and "password" (<tt>xs:string</tt>) are mandatory.

You can supply 2 additional options:

- "database" (<tt>xs:string</tt>) : alternative database name, if not using the one associated with the HTTP server.
- "authentication" (<tt>xs:string</tt>) : authentication method. Authorized values: "digest", "basic" (default).

There is also a third parameter that can be supplied as a XPath 3.0 map containing the external variables values. The map must be of type <tt>map(xs:QName, item()?)</tt>, where :

- each key is a <tt>xs:QName</tt> matching an external variable declaration in the XQuery script ;
- each value must be a singleton or empty sequence (because of restrictions in the MarkLogic Java API).

Most of the XDM atomic and node types are supported (including maps and arrays), though there might be some unsupported ones or restrictions of usage.
For instance, an <tt>empty-sequence()</tt> is sent as a <tt>xs:anyAtomicType("")</tt> to MarkLogic and thus cannot be casted as a node in the XQuery.
Also, <tt>attribute()</tt>, <tt>comment()</tt> and <tt>processing-instruction()</tt> will be sent to MarkLogic wrapped in a dummy document element.
If you want to pass multiple atomic values, use an <tt>array</tt>. For multiple nodes, wrap them inside a dummy <tt>element()</tt>.

Example :

<pre>&lt;xsl:sequence xmlns:mkl-ext="fr:askjadev:xml:extfunctions" select="
  mkl-ext:marklogic-query-uri(
    'file:/path/to/file.xqy',
    $configMap,
    map{
       QName('http://namespace','pre:string'):'string value',
       QName('http://namespace','pre:integer'):1,
       QName('http://namespace','pre:comment'):$comment
    }
  )"
/&gt;
</pre>

<pre>declare namespace pre="http://namespace";
declare variable $pre:string as xs:string external;
declare variable $pre:integer as xs:string external;
declare variable $pre:comment as document-node() external;
let $pre:comment := $pre:comment/comment()
[...]
</pre>

The query can return node(s) (except attributes) or atomic value(s), though there might be some unsupported ones or restrictions of usage.


## Current version (for Saxon 9.9/9.8): 1.4.0

Maven support:

<pre>
&lt;dependency&gt;
  &lt;groupId&gt;fr.askjadev.xml.extfunctions&lt;/groupId&gt;
  &lt;artifactId&gt;marklogic&lt;/artifactId&gt;
  &lt;version&gt;1.4.0&lt;/version&gt;
&lt;/dependency&gt;
</pre>


## Build

To build the project from the sources, follow these steps:

<pre>
$ git clone https://github.com/AxelCourt/saxon-marklogic-ext.git
$ cd saxon-marklogic-ext
$ mvn clean package -DskipTests=true
</pre>

Please note that you need to deactivate the tests using the parameter `-DskipTests=true` to be able to build the project, unless you have correctly configured your MarkLogic Server environment.


## Testing

The tests require a running MarkLogic Server instance. By default, they are run under the following MarkLogic Server configuration:

* MarkLogic Server runs on `localhost`.
* There is a `Test` database associated with a HTTP Server on port `8004`.
* Username/password are `admin`/`admin`.
* User needs to be rest-admin in MarkLogic.
* The HTTP Server authentication scheme is `basic`.

If you wish to change this behaviour, you can add additional parameters to the test command-line which values will be used instead of the default ones.

|Parameter|Default values|Usage|Description|
|----|----|----|----|
|testServer|localhost|`-DtestServer=10.11.12.90`|The server on which to run the tests.|
|testPort|8004|`-DtestPort=8999`|The port to use to talk to the HTTP Server.|
|testUser|admin|`-DtestUser=myUser`|An authorised user.|
|testPassword|admin|`-DtestPassword=myPassword`|The user password.|


## Thanks

Many thanks to Christophe Marchand for the base code, and Emmanuel Tourdot for the improvements!

Go there for a BaseX similar extension function: <a href="https://github.com/cmarchand/xpath-basex-ext">https://github.com/cmarchand/xpath-basex-ext</a>.
