# **L**ive **O**wl **D**ocument **E**nvironment

This code is a fork of the original LODE source created by [Silvio Peroni](http://palindrom.es/phd/whoami/) and hosted at [https://github.com/essepuntato/LODE](https://github.com/essepuntato/LODE).

The **master** branch of this repository contains Peroni's original source code, as pulled from his GitHub repository. 
It defines a _lode_ Java servlet that is able to create a HTML-formatted Ontology description from an OWL-encoded ontology definitition file that is internet-accessible.
The canonical version of this service is hosted at [http://www.essepuntato.it/lode]. The way the canonical service accepts request parameters (in the URL path) is different from how the serlet code expects to handle them (as standard querystring parameters), suggesting that there is a proxy layer between the two.

The **refactor** branch of this repository is the active one for our purposes. It includes the following broad modifications to the source code:

- Convert to use the Java 7 SDK and Apache Tomcat 7.
- Compile and run in Eclipse Luna, and able to be debugged in development using the above SDK and service.
- Add an ANT build script to allow compilation and testing without using the Eclipse IDE (e.g. by a Jenkins build server)
- Refactor the LodeServlet class to split servlet-specific functionality away from an API class, to remove unneeded code and improve error handling.
- Support parsing of local ontology definition files as well as internet-hosted ones (enabled or not via context parameter).
- Supply a proper servlet 'home' page as a JSP that contains a form to submit requests to the LODE servlet, and gives documentation for how the servlet may be called using querystring parameters (i.e. without needing the proxy layer that the canonical server apparently has).
- Add a command-line server ( **LodeCmd** class) so that the LODE API can be used without actually needing to host the servlet.
- Add an alternate build configuration that can create an executable JAR file instead of a Tomcat-hosted WAR file.   



example usage:

1. launch application with

	mvn clean jetty:run

2. test /extract

	+ DOLCE ontology
		
		http://localhost:8080/lode/extract?url=http://www.loa.istc.cnr.it/ontologies/DOLCE-Lite.owl
	
	+ photography ontology
	
		http://localhost:8080/lode/extract?url=http://130.88.198.11/co-ode-files/ontologies/photography.owl