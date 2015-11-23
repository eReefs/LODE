# **L**ive **O**wl **D**ocument **E**nvironment

The LODE application was created by [Silvio Peroni](http://www.essepuntato.it/) and the canonical version of the source code is hosted at [https://github.com/essepuntato/LODE](https://github.com/essepuntato/LODE).

It is a Java Application that can be compiled either as a Web Application (for deployment into a servlet container) or as a .jar file (for use on the command line). It uses [Maven](https://maven.apache.org/) to specify non-local dependencies.

Both modes allow a caller to request a HTML-formatted description of a formal ontology definition, where the definition is given in a parsable [OWL](http://www.w3.org/2001/sw/wiki/OWL) format (RDF, Turtle etc).

## LODE Web Application 
The LODE application can be compiled into a .war file for deployment into a Java Servlet container such as [Jetty](http://www.eclipse.org/jetty/), [Tomcat](http://tomcat.apache.org/) or [Glassfish](https://glassfish.java.net/).  It defines two servlets.

- The **source** servlet can render an Ontology definition document as plain text. It is accessed as:
    
    http://example.com/lode/source?url=[url-encoded-ontology-definition-url]

- The **extract** servlet can generate an HTML description from an Ontology definition document. It is accessed like: 

    http://example.com/lode/extract?[optional-parameter-key]=[optional-parameter-value]&url=[url-encoded-ontology-definition-url] (see below for a list of the possible parameters).

- The home page for the LODE Servlet Application is a JSP page that describes how to use the application. It includes a form for generating LODE-extract URLs. 

- The standard web application path is **/lode**,  but this is not mandatory - you can deploy to a different path (or the ROOT path) if you chose.

- The ontology definition file *and* any other files it references must be accessible via HTTP to be transformed via a LODE Servlet Instance.

## LODE Command Line
The LODE application can *also* be used from the command line, in order to create static HTML description files for local ontology definitions. 

It allows you to map the eventual ontology URLs to local file paths so that the descriptions can be created before the ontology definitions are deployed.

This command line tool is useful when you want to be able to cache a limited set on descriptions for fast subsequent access, rather than deploy and secure a standard LODE web application.

Command line calls will look like:
   
    java -jar /path/to/LODE.jar 
        -url "[ontology-definition-url]" 
        -path "[/path/to/local/ontology/definition/file]" 
        -html "[/path/to/save/ontology/description/html]"

## LODE Transfomation Arguments

The following arguments can always be used when transforming a LODE ontology:

| Transform Parameter | Type | Meaning |
|---------------------|------|---------|
| **url** | String | **Required.** The full http://... URL of the OWL ontology that will be processed by the service. This is referred to as *ontology-url* in this documentation. |
| **owlapi** | Boolean | **true** (the default) if the ontology should be pre-processed using the [OWLAPI](http://owlapi.sourceforge.net/) in order to linearized it in standard RDF/XML format. Set this to **false** to skip this preprocessing step. |
| **closure** | Boolean | When set to **true**, the transitive closure given by considering the imported ontologies of *ontology-url* is added to the HTML description of the ontology. This parameter implicitly specifies the owlapi parameter. The default is **false**. |
| **imported** | Boolean | When set to **true**, the axioms contained the ontologies directed imported into *ontology-url* are added to the HTML description of the ontology. This parameter implicitly specifies the owlapi parameter. If both *closure* and *imported* are specified (in any order), only *imported* will be considered. The default is **false** |
| **reasoner** | Boolean | When set to **true**, the assertions inferable from *ontology-url* using the [Pellet reasoner](http://clarkparsia.com/pellet) will be added to the HTML description of the ontology. This parameter implicitly specifies the owlapi parameter. Note that, depending upon the nature of your ontology, this computationally intensive function can be very time-consuming. The default is **false** |
| **lang** | String | The language to use when showing annotations of the ontology specified in *ontology-url*. Currently supported values are **en** (English), **fr** (French) and **it** (Italian). The default is **en**. |


The following arguments behave differently depdending on how your LODE application is deployed:

| Parameter | Type | Meaning |
|-----------|------|---------|
| **source** | String | The URL to be used for the *Ontology Source* link included in each HTML-formatted Ontology description. If not provided, then the command-line application will attempt to link directly to the source of the *ontology-url*, but the web application will generate a URL that passes the *ontology-url* to the local source servlet. |
| **imports** | String | Mappings between the canonical URL for an ontology dependency and the URI from which that ontology definition should *actually* be read, Formatted as **url_1**=*file_1*\|**url_2**=*file_2*\|...\|**url_n**=*file_n*.  For the command-line application, URLs may be mapped to local files, which is handle if the definitions are not yet accessible at the URL in question.  For the web application, you may only map URLs to other remote URLs. |
| **lodeBase** | String | The base URL to which the *ontology-url* should be appended for any 'Visualise with Lode' links. The command-line application can accept a value for this like any other parameter, and will use the the canonical LODE instance at *http://www.essepuntato.it/lode/owlapi/* if no value is provided. The Web Application defaults to the base URL for the local extract servlet with owlapi parsing enabled (/lode/extract?owlapi=true&url=), but allows this to be overridden by a global context parameter (in WEB-INF/web.xml or other context override location) in the event that you with to specify different arguments. |


And this final set of arguments is *only* available when using the LODE application on the command line:

| Command-line Parameter | Type | Meaning |
|------------------------|------|---------|
| **html** | String | **Required.** Absolute path to the local filesystem location where the HTML description of the ontology definition should be saved. Any stylesheets and javascript files referenced by the HTML description will be saved below a *lode* directory in the same location as the HTML file. |
| **path** | String | Absolute path to the ontology definition file on the local filesystem. If this is not provided, then the ontology definition will be downloaded from from the *ontology-url*. |


## Development Environment

Work on the LODE source code using maven and the Eclipse IDE. To load the code into Eclipse:

- Download and install the Eclipse IDE.
- Ensure the [m2Eclipse](http://www.eclipse.org/m2e/) (Maven-Eclipse integration) Plugin is installed.
- Ensure a Java 8 JDK is installed and configured as a System JRE.
- Choose **File -> Import -> Maven -> Existing Maven Projects** from the menu. Browse to the location of the LODE source code (the pom.xml file is in the root of the source code directory).

Loading and building the project this way should trigger a download of all the Maven dependencies. If you need to change these dependencies (by editing the pom.xml file), then you can right-click the project and choose **Maven -> Update Project** to refresh everything.


### Testing the LODE Servlet


example usage:

1. launch application with

	mvn clean jetty:run

2. test /extract

	+ DOLCE ontology
		
		http://localhost:8080/lode/extract?url=http://www.loa.istc.cnr.it/ontologies/DOLCE-Lite.owl
	
	+ photography ontology
	
		http://localhost:8080/lode/extract?url=http://130.88.198.11/co-ode-files/ontologies/photography.owl
		
### Debugging the LODE Servlet with Jetty from within Eclipse

- Use either the [run-jetty-run](https://code.google.com/p/run-jetty-run/) Eclipse Plugin or the [Eclipse Jetty Integration](http://eclipse-jetty.github.io/) Plugin.

- Create a special debug launch configuration under **Run-> Debug Configurations -> Jetty Webapp**.

- Choose your LODE project as the Eclipse project to launch, Set the webapp folder to **src/main/webapp** and the context path to **/lode**.

- Choose a local  http port that is free for use on your development machine (e.g. **8080**).

- Click **Debug** to launch the debug configuration.

- Open a browser, and navigate to http://localhost:**8080/lode**, which will call up the index page for your lode instance. This gives you a starting point for calling up assorted test URLs. Any breakpoints in the servlet source code will work as URLs are called up.

### Debugging the LODE Server from within Eclipse

- Create a special Debug Launch configuration under **Run -> Debug Configurations -> Java Application**.

- Choose your LODE project as the Eclipse project to launch, and set **it.essepuntato.lode.LodeApp** as the main class.

- In the **arguments** tab of the configuration setup dialog, enter the arguments for your call to the LODE Server. For example, assuming you have a local copy of the 'earmark' ontology and the ghost ontology that it depends on saved at **D:\lode_test\**, your arguments might be:

	-url "http://www.essepuntato.it/2008/12/earmark"
	-path "D:\lode-test\earmark.ttl"
	-html "D:\lode-test\earmark.htm"
	-source "earmark.rdf"
	-imports "http://www.essepuntato.it/2010/05/ghost=D:\lode-test\ghost.rdf"

- Click **Debug** to launch the debug configuration. Your LODE project will be launched in the debugger, calling the **main** function in the **LodeApp** class.

### Continuous Integration for the LODE Applications in Jenkins

The LODE project contains an ANT build project that has been created specifically to support Jenkins builds. It uses the [Maven Ant Tasks](http://maven.apache.org/ant-tasks/index.html) library to make all the maven dependencies available to the ANT tasks.

Your Jenkins server will need to have Java 1.8, Ant and Maven available, and your Jenkins instance should have the Environment Injector plugin installed.

Your first Build step should inject two environment variables (substitute appropriate values):

    IMPLEMENTATION_VERSION=[version number]
    IMPLEMENTATION_VENDOR=[your company name]
    
Subsequent build steps can call appropriate ANT tasks:

- **compile** to just compile the source code.
- ** war** to package up a .war file for servlet deployment at ./out/LODE-[version number].war
- **jar** to assemble the .jar file and all its dependencies for command-line use at ./build/jar/LODE-[version number].jar, and package them all up to a tarball at ./out/LODE-[version number]_cmd-line.tar.gz
- **do_cmd_line** to test the results of the **jar** task on a local ontology document.

