# **L**ive **O**wl **D**ocument **E**nvironment

The LODE application was created by [Silvio Peroni](http://www.essepuntato.it/) and the canonical version of the source code is hosted at [https://github.com/essepuntato/LODE](https://github.com/essepuntato/LODE).

It is a Java Application that can be compiled either as a Web Application (for deployment into a servlet container) or as a .jar file (for use on the command line). It uses [Maven](https://maven.apache.org/) to specify non-local dependencies, and has an ANT build script to package the build results into each of the two deployment configurations. This script uses the [Maven Ant Tasks](http://maven.apache.org/ant-tasks/index.html) library to make all the maven dependencies available to ANT.

Both modes allow a caller to request a HTML-formatted description of a formal ontology definition, where the definition is given in a parsable 
[OWL](http://www.w3.org/2001/sw/wiki/OWL) format (RDF, Turtle etc). Depending on the parameters specified, the HTML pages created by LODE takes into account both ontological axioms and annotations. The CSSs used for the resulting HTML page are based on the W3C CSSs for Recommendation documents. 

## Development Environment

The LODE source code is structured for use with the Eclipse IDE. To load the code into Eclipse:

- Download and install the Eclipse IDE.
- Ensure the [m2Eclipse](http://www.eclipse.org/m2e/) (Maven-Eclipse integration) Plugin is installed.
- Ensure a Java 8 JDK is installed and configured as a System JRE.
- Choose **File -> Import -> Maven -> Existing Maven Projects** from the menu. Browse to the location of the LODE source code (the pom.xml file is in the root of the source code directory).

Loading and building the project this way should trigger a download of all the Maven dependencies. If you need to change these dependencies (by editing the pom.xml file), then you can right-click the project and choose **Maven -> Update Project** to refresh everything.


## LODE Web Application 
The LODE application can be compiled into a .war file for deployment into a Java Servlet container such as [Jetty](http://www.eclipse.org/jetty/), [Tomcat](http://tomcat.apache.org/) or [Glassfish](https://glassfish.java.net/).  It defines two servlets, and provides a JSP index page that includes dpcumentation about how to use the application.

### The **source** servlet 

Can render the contents of an ontology definition document in the response body with a text/plain MIME type. as plain text. It is accessed like so: 
    
    http://example.com/lode/source?url=[url-encoded-ontology-definition-url]

### The **extract** servlet 

Actually does the work of transforming an ontology definition into an HTML-formatted description. It is accessed like: 

    http://example.com/lode/extract?[optional-parameter-key]=[optional-parameter-value]&url=[url-encoded-ontology-definition-url]
    
This servlet accepts the following request/querystring parameters:

| Request Parameter | Meaning |
|---------------------|------|---------|
| **url**=*[url-encoded-iri]* | **Required.** The absolute HTTP IRI/URL of the OWL ontology that will be processed by the service. This is referred to as *ontology-url* in this documentation. |
| **imports**=[**iri_1**=*url_1*\|...\|**iri_n**=*url_n*] | Mappings between the canonical IRI for an ontology dependency and the *absolute* URL from which that ontology definition should *actually* be read |
| **owlapi**=*[true\|false]* | When set to **true**, the *ontology-url* will be pre-processed via [OWLAPI](http://owlapi.sourceforge.net/), in order to linearized it in standard RDF/XML format. If you know your ontology definition is *already* valid RDF/XML, you can set this to *false* to skip the validation step. The default is **true**  |
| **closure**=*[true\|false]* | When set to **true**, the transitive closure given by considering the imported ontologies of *ontology-url* is added to the HTML description of the ontology. This parameter implicitly specifies the *owlapi* parameter. The default is **false**.  |
| **imported**=*[true\|false]* | When set to **true**, the axioms contained in the imported ontologies of *ontology-url* are added to the HTML description of the ontology. This parameter implicitly specifies the *owlapi* parameter. If both *closure* and *imported* are specified (in any order), only *imported* will be considered. The default is **false** |
| **reasoner**=*[true\|false]* | When set to **true**, the assertions inferrable from *ontology-url* using the [Pellet reasoner](http://clarkparsia.com/pellet) will be added to the HTML description of the ontology. Note that, depending upon the nature of your ontology, this computationally intensive function can be very time-consuming. This parameter implicitly specifies the *owlapi* parameter. The default is **false** |
| **lang**=*[en\|fr\|it]* | The language-code for the language to use for headings and other common content in the ontology-description HTML. *ontology-url*. Currently supported codes are **en** (English), **fr** (French) and **it** (Italian). The default is **en**. |
    

### Testing the LODE Web Application

example usage:

1. launch application with

	mvn clean jetty:run

2. test /extract

	+ DOLCE ontology
		
		http://localhost:8080/lode/extract?url=http://www.loa.istc.cnr.it/ontologies/DOLCE-Lite.owl
	
	+ photography ontology
	
		http://localhost:8080/lode/extract?url=http://130.88.198.11/co-ode-files/ontologies/photography.owl
		
### Debugging the LODE Web Application with Jetty from within Eclipse

- Use either the [run-jetty-run](https://code.google.com/p/run-jetty-run/) Eclipse Plugin or the [Eclipse Jetty Integration](http://eclipse-jetty.github.io/) Plugin.

- Create a special debug launch configuration under **Run-> Debug Configurations -> Jetty Webapp**.

- Choose your LODE project as the Eclipse project to launch, Set the webapp folder to ```src/main/webapp``` and the context path to ```/lode```.

- Choose a local  http port that is free for use on your development machine (e.g. **8080**).

- Click **Debug** to launch the debug configuration.

- Open a browser, and navigate to http://localhost:**8080/lode**, which will call up the index page for your lode instance. This gives you a starting point for calling up assorted test URLs. Any breakpoints in the servlet source code will work as URLs are called up.

### Web Application Package Build

- For a production build, define two environment variables (substitute appropriate values) to indicate the origins of the package.
```
    IMPLEMENTATION_VERSION=[version number]
    IMPLEMENTATION_VENDOR=[your company name]
 ```   
- Run the **war** Ant task. This will update the Maven dependencies, compile the source code and assemble the .war file, which will be placed in ```./out/LODE-[version number].war```


### Web Application Deployment

The standard servlet container deployment path is **/lode**,  but this is not mandatory - you can deploy to a different path (or the ROOT path) if you chose.

You can provide overrides for the following context parameters (in WEB-INF/web.xml or other context locations) to change some aspects of how the application behaves:

| Context Parameter | Type | Meaning |
|-----------|------|---------|
| **cssExtra** | String | The URL of an additional stylesheet that should be referenced by the HTML ontology descriptions. Useful if you'd like your descriptions to use a house style, rather than looking like all other LODE-generated descriptions. The default is empty, for no extra stylesheet. |
| **maxRedirects** | Integer | The maximum number of redirects that should be followed when retrieving the source of an ontology description. The default is 50, but you can override this if you want problem URLs to fail faster, thus putting less of a drain on server resources. (Bad source requests are a DDOS vulnerability) |
| **sourceBase** | String | The base URL to which the *ontology-url* should be appended for any 'view source' links in the HTML description. Defaults to the source-servlet base ```./source?url=```, but you can override it here if you have a proxy mapping that changes how source-requests should be formatted, or if you wish to use a different service to retrieve the definition source. |
| **visBase** | String | The base URL to which the IRI of an imported ontology can appended when constructing 'Visualise it with Lode' links. The default is the extract-servlet base ```./extract?owlapi=true&url=```, but you can override it here if you have a proxy mapping that changes how transform-requests should be formatted. |
| **lodeHome** | String | The URL for the LODE homepage, to be used in documentation to link back to the application which generated an ontology description. Defaults to the local LODE index page ```index.jsp```, but you can override it here to use a different 'home' URL. |


## LODE Command Line
The LODE application can also be used from the command line, in order to create static HTML description files for local ontology definitions. This can be used as a build tool when producing archives of ontology definition-and-description files to be statically hosted in another location. 

It permits the ontology definition files to be read from the local filesystem as well as from remote URLs by allowing you to map official ontology IRIs to local file paths (including for imported ontologies). This means that the documents can still be parsed and transformed before the ontology definitions are deployed.

The command line application accepts the following arguments:

| Command Line Argument | Meaning |
|---------------------|------|---------|
| **-url**=_[ontology-IRI]_ |  **Required.** The HTTP IRI for the OWL ontology that you want to process. This is referred to as *ontology-url* in this documentation. |
| **-html**=_[/path/to/save/ontology.htm]_ | **Required.** Absolute path to the local filesystem location where the HTML description of the ontology definition should be saved. |
| **-path**=_[/path/to/read/ontology.ttl]_ | Optional filesystem path from which the ontology definition should actually be read. If this argument is not provided, then the ontology definition will be downloaded from from the _ontology-url_. |
| **-imports**=[**iri1**=_/path/to/ref1_\|...\|**iriN**=_/path/to/refN_] | Mappings between the canonical IRI for an ontology dependency and the URI from which that definition should actually be read. URIs may be alternate absolute URLs, *or* local filesystem paths (if dependencies are not yet published) |
| **-closure** | When this argument is present, the transitive closure given by considering the imported ontologies of *ontology-url* is added to the HTML description of the ontology. |
| **-imported** | When this argument is present, the axioms contained in the imported ontologies of *ontology-url* are added to the HTML description of the ontology. If both *-closure* and *-imported* are specified (in any order), only *-imported* will be considered. |
| **-reasoner** | When this argument is present, the assertions inferrable from *ontology-url* using the [Pellet reasoner](http://clarkparsia.com/pellet) will be added to the HTML description of the ontology. This can be slow. |
| **-lang**=_[en\|fr\|it]_ | The language-code for the language to use for headings and other common content in the ontology-description HTML. _ontology-url_. Currently supported codes are **en** (English), **fr** (French) and **it** (Italian). The default is **en**. |
| **-cssBase**=_[./rel/path/to/css]_ | The *relative* path from the value of *html* to where the stylesheets and javascript files needed bythe HTML ontology description should be placed. If this argument is not present, an default path of ```./lode/``` will be used.
| **-cssExtra**=_[http://example.com/my-style.css]_ | URL for an additional stylesheet that HTML ontology descriptions should reference. |
| **-source**=*[http://example.com/ontology.rdf]* | The absolute or relative link to reference for the **show source** link in the ontology description. | 
| **-saveSource** | When this argument is present, an RDF/XML version of the ontology definition will be saved to the same directory given for *html*, and referenced by the **show source** link in the generated HTML description. Ignored if the *source* argument is also provided. |
| **-sourceBase**=_[http://example.com/lode/source?url=]_ | The base URL to which the *ontology-ul* can be appended in order to construct the **show source** link in the generated HTML description. Ignored if either the *source* or the *saveSource* arguments are provided. Defaults to the *source* servlet used by the canonical LODE server: ```http://eelst.cs.unibo.it/apps/LODE/source?url=``` |
| **-visBase**=_[http://example.com/lode/extract?url=]_ | The base URL to which the the IRI of an imported ontology can appended when constructing 'Visualise it with Lode' links. Defaults to the canonical LODE service: ```http://www.essepuntato.it/lode/owlapi/``` |
| **-lodeHome**=_[http://example.com/lode/]_ |The URL for the LODE homepage, to be used to link back to the LODE documentation from HTML ontology descriptions. Defaults to the canonical LODE homepage: ```http://www.essepuntato.it/lode/``` |


Command line calls will look like:
   
    java -jar /path/to/LODE.jar 
        -url "http://example.com/example-ontology" 
        -path "/path/to/local/example-ontology.ttl"
        -imports "http://example.com/referenced-ontology=/path/to/local/referenced-ontology.ttl" 
        -html "/path/to/save/example-ontology.htm"
        -saveSource
        
This particular example will read the turtle-formatted **example-ontology** definition (and the **referenced-ontology** that it imports) from the local filesystem, and will save the HTML-formatted description at ```/path/to/save/example-ontology.htm```. The CSS and other client side files referenced by the HTML document will be saved below ```/path/to/save/lode/``` (this location is configurable). Because the **-saveSource** argument was specified, and RDF/XML version of the ontology definition will *also* be saved as a sibling of the HTML file (at ```/path/to/save/example-ontology.rdf```), and will be referenced in the 'Show Source' link.  

### Debugging the LODE Command Line from within Eclipse

- Create a special Debug Launch configuration under **Run -> Debug Configurations -> Java Application**.

- Choose your LODE project as the Eclipse project to launch, and set ```it.essepuntato.lode.LodeApp``` as the main class.

- In the **arguments** tab of the configuration setup dialog, enter the arguments for your call to the LODE Server. For example, assuming you have a local copy of the 'earmark' ontology and the ghost ontology that it depends on saved at **D:\lode_test\**, your arguments might be:

	-url "http://www.essepuntato.it/2008/12/earmark"
	-path "D:\lode-test\earmark.ttl"
	-html "D:\lode-test\earmark.htm"
	-source "earmark.rdf"
	-imports "http://www.essepuntato.it/2010/05/ghost=D:\lode-test\ghost.rdf"

- Click **Debug** to launch the debug configuration. Your LODE project will be launched in the debugger, calling the **main** function in the **LodeApp** class.

### Command Line Package Build

- For a production build, define two environment variables (substitute appropriate values) to indicate the origins of the package.
```
    IMPLEMENTATION_VERSION=[version number]
    IMPLEMENTATION_VENDOR=[your company name]
 ```   
- Run the **jar** Ant task. This will update the Maven dependencies, compile the jar file to ```./build/jar/LODE-[version number].jar``` , and then package it up with all the required file and library dependencies to ```./out/LODE-[version number]_cmd-line.tar.gz```.

- Run the **do_cmd_line** Ant task to test the packaged .jar file against a local copy of an ontology definition file.



