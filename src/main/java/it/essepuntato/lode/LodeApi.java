/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *      
 * Copyright (c) 2010-2013, Silvio Peroni <essepuntato@gmail.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package it.essepuntato.lode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.mindswap.pellet.PelletOptions;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.semanticweb.owlapi.util.InferredDisjointClassesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentDataPropertiesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentObjectPropertyAxiomGenerator;
import org.semanticweb.owlapi.util.InferredInverseObjectPropertiesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredPropertyAssertionGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubDataPropertyAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubObjectPropertyAxiomGenerator;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

/**
 * LODE API Class
 * Encapsulates the LODE functionality which is independent of the type of application.
 */
public class LodeApi {
	/* Properties to be read from the local manifest. */
	public final String vendorName;
	public final String vendorUrl;
	public final String appVersion; 
	public final String buildDate;
	public final String defaultSourceBase;
	public final String defaultVisBase;
	public final String defaultLodeHome;
	
	/* Properties that depend on LODE deployment */
	public final String xsltPath;
	public final URI pelletPropertiesUri;
	public final UriHelper.UriPathType definitionPathType;
	public final int maxRedirects;
	public final String userAgent;
	public final String cssBase;
	public final String sourceBase;
	public final String visBase;
	public final String lodeHome;

	/* Useful constants */
	public final String[] mimeTypes = {
		"application/rdf+xml" ,
		"text/turtle" , 
		"application/x-turtle",
		"text/xml" ,
		"text/plain", 
		"*/*"
	};
	public final String[] languages = {
	    "en",
		"fr",
		"it"
	};
	
    /**
     * Initialising constructor for the {@link LodeApi} type.
     * @param resourcePath			The local filesystem location that contains the directories of server and client files.
     * @param allowLocalDefinitions	Whether this LODE instance is permitted to read ontology definitions from the local filesystem.
     * @param maxRedirects			The maximum number of redirects to follow when downloading ontology definitions before assuming an error.
     * @param userAgent				The user agent string that should identify this application when downloading ontology definitions. 
     * @param cssBase				The base URL from which ontology descriptions should reference client-side resources like stylesheets.
     * 								Use null if client files have the same web-path as generated ontology descriptions.
     * @param sourceBase			The base URL to which an ontology-definition URL can be appended in order to reference the plain-text
     * 								source of the definition. Use null to skip generating source links in ontology descriptions this way.
     * @param visBase				The base URL to which an ontology-definition URL can be appended in order to reference a LODE-generated 
     * 								description of that ontology. Use null to skip generating 'Visualise it with LODE' links in ontology descriptions.
     * @param lodeHome				The URL of the LODE web application, used in the footer link of Ontology Descriptions.
     * @throws LodeException		If any of the parameters are invalid.
     */
    public LodeApi(
    		String resourcePath, 
    		boolean allowLocalDefinitions,
    		int maxRedirects,
    		String userAgent,
    		String cssBase,
    		String sourceBase,
    		String visBase,
    		String lodeHome
    	) throws LodeException {
    	
    	try {   		
    		// Parse manifest properties.
    		URI manifestUri = UriHelper.validateURI(resourcePath + "/META-INF/MANIFEST.MF", UriHelper.UriPathType.Filesystem, false, "Invalid Manifest Path");
     		try (InputStream manifestStream = manifestUri.toURL().openStream() ){
        		Properties prop = new java.util.Properties();
        		prop.load(manifestStream);
        		this.vendorName = prop.getProperty("Specification-Vendor");
        		this.vendorUrl = prop.getProperty("Specification-Vendor-Url");
        		String impVersion = prop.getProperty("Implementation-Version");
        		this.appVersion =  (impVersion == null || impVersion.isEmpty()) ? prop.getProperty("Specification-Version") : impVersion;
        		this.buildDate = prop.getProperty("Build-Date");
        		this.defaultSourceBase = prop.getProperty("Source-Base");
        		this.defaultVisBase = prop.getProperty("Vis-Base");
        		this.defaultLodeHome = prop.getProperty("Specification-Url");
    		}
    		
     		// Validate and cache deployment-specific properties.
     		this.xsltPath = UriHelper.validatePath(resourcePath +  "/server/extraction.xsl", UriHelper.UriPathType.Filesystem, false, "Invalid XSLT path");
        	this.pelletPropertiesUri = UriHelper.validateURI(resourcePath + "/server/pellet.properties",  UriHelper.UriPathType.Filesystem, false, "Invalid pellet.properties path");
        	this.definitionPathType = allowLocalDefinitions ? UriHelper.UriPathType.Any : UriHelper.UriPathType.WebAbsolute;
        	this.maxRedirects = maxRedirects;
        	this.userAgent = userAgent;
    		this.cssBase = UriHelper.validatePath(cssBase.replaceAll("([^\\/])$", "$1/"), UriHelper.UriPathType.WebAny, false, "Invalid CSS base URL");
 
    		String useSourceBase = UriHelper.validatePath(sourceBase, UriHelper.UriPathType.WebAny, true, "Invalid Source link base URL");
    		this.sourceBase = useSourceBase.isEmpty() ? this.defaultSourceBase : useSourceBase;

    		String useVisBase = UriHelper.validatePath(visBase, UriHelper.UriPathType.WebAny, true, "Invalid Visualise link base URL");
    		this.visBase = useVisBase.isEmpty() ? this.defaultVisBase : useVisBase;
     	
    		String useLodeHome = UriHelper.validatePath(lodeHome, UriHelper.UriPathType.WebAny, true, "Invalid LODE Homepage URL");
    		this.lodeHome = useLodeHome.isEmpty() ? this.defaultLodeHome : useLodeHome;
    	}
    	catch(Exception ex){
    		throw new LodeException(ex);
    	}
    }
    
    /**
     * Initialising constructor for the {@link LodeApi} type in a web-application context.
     * This derives all base paths and URLs from the context properties, and does NOT
     * permit ontology definitions to be read directly from the local filesystem, since
     * that would be a horrible security risk.
     * @param context					information about the web application environment.
     * @throws NumberFormatException	if the maxRedirects context parameter is non-numeric.
     * @throws LodeException			if any of the derived context properties are invalid.
     */
    public LodeApi(ServletContext context, String userAgent) throws NumberFormatException, LodeException{
		this( context.getRealPath("."), 
			false, 
			Integer.parseInt(context.getInitParameter("maxRedirects")), 
			userAgent,
			"client/",
			context.getInitParameter("sourceBase").isEmpty() ? "source?url=" : context.getInitParameter("sourceBase"),
			context.getInitParameter("visBase").isEmpty() ? "extract?owlapi=true&url=" : context.getInitParameter("visBase"),					
			context.getInitParameter("lodeHome").isEmpty() ? "index.jsp" : context.getInitParameter("lodeHome")
		);
    }
    
    
    /**
     * Read or download the contents of an ontology definition into a String.
     * This function does NOT make any attempt to parse or validate the ontology definition.
     * @param ontologyPath		The local path or remote URL for the ontology definition to read.
     * @return					A string containing the contents of the ontology definition.
     * @throws LodeException	If the ontology definition cannot be read.
     */
    public String getOntologySource(String ontologyPath) throws LodeException{
    	try {
    		URI uri = UriHelper.validateURI(ontologyPath, this.definitionPathType, false, "Invalid Ontology Source URI");
    		String content = UriHelper.getSource(uri, Arrays.asList(this.mimeTypes), this.userAgent, this.maxRedirects);
    		return content;
    	}
    	catch(Exception ex){
    		throw new LodeException(ex);
    	}
    }
    
	private static void applyAnnotations(
		OWLEntity aEntity, 
		Map<OWLEntity, Set<OWLAnnotationAssertionAxiom>> entityAnnotations, 
		OWLOntologyManager manager, 
		OWLOntology ontology
	) {
		Set<OWLAnnotationAssertionAxiom> entitySet = entityAnnotations.get(aEntity);
    	if (entitySet != null) {
    		for (OWLAnnotationAssertionAxiom ann : entitySet) {
    			manager.addAxiom(ontology, ann);
    		}
    	}
	}
    
	/**
	 * Parse an Ontology Definition document using the [OWLAPI](http://owlapi.sourceforge.net/) to linearize it in standard RDF/XML format.
	 * @param ontologyPath	The location of the ontology definition document to be parsed.
	 * @param imports		An optional mapping of imported ontology definition URIs to alternate locations from which
	 * 						those imported definitions should be read. 
	 * @param imported		Whether the axioms contained in any imported ontologies should be included. 
	 * @param closure		Whether the transitive closure given by considering any imported ontologies should be included.
	 * 						The closure parameter is ignored if imported is true.
	 * @param reasoner		Whether the assertions inferable from the ontology definition using the [Pellet reasoner](http://clarkparsia.com/pellet) 
	 * 						should be included. Note that this can be very time-consuming.
	 * @return the read/downloaded and parsed ontology definition in standard RDF/XML format.
	 * @throws LodeException
	 */
	public String parseOntology(
		String ontologyPath,
		String imports,
		boolean imported, 
		boolean closure,
		boolean reasoner 
	) throws LodeException{
		try {
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			
			Map<URL, URI> ontologyImports = UriHelper.parseUriMap(imports, this.definitionPathType);
			if (ontologyImports != null){
				for (Map.Entry<URL,URI> mapEntry : ontologyImports.entrySet()){
					manager.addIRIMapper(new SimpleIRIMapper(IRI.create(mapEntry.getKey()), IRI.create(mapEntry.getValue())));
				}
			}
					
			URI ontologyUri = UriHelper.validateURI(ontologyPath, this.definitionPathType, false, "Invalid Ontology Definition URI");
			IRI ontologyIri = IRI.create(ontologyUri);
			HttpURLConnection.setFollowRedirects(true);
			OWLOntology ontology = null;
					
			if (imported){
				// We want to consider the imported ontology definitions when parsing.
				ontology = manager.loadOntology(ontologyIri);
				Set<OWLOntology> importedOntologies = new HashSet<OWLOntology>();
				importedOntologies.addAll(ontology.getDirectImports());
				for (OWLOntology importedOntology : importedOntologies) {
					manager.addAxioms(ontology, importedOntology.getAxioms());
				}
			}
			else if (closure){
				// Consider the imported ontology closures when parsing.
				ontology = manager.loadOntology(ontologyIri);
				Set<OWLOntology> importedClosures = new HashSet<OWLOntology>();
				importedClosures.addAll(ontology.getImportsClosure());
				for (OWLOntology importedClosure : importedClosures) {
					manager.addAxioms(ontology, importedClosure.getAxioms());
				}
			} 
			else {
				// Ignore the imported ontologies when parsing.
				IRIDocumentSource source = new IRIDocumentSource(ontologyIri);
				OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
				ontology = manager.loadOntologyFromOntologyDocument(source, config);
			}
			
			StringDocumentTarget parsedOntology = new StringDocumentTarget();        	
        	if (reasoner){
        		
    			PelletOptions.load(this.pelletPropertiesUri.toURL());
    			PelletReasoner pelletReasoner = PelletReasonerFactory.getInstance().createReasoner(ontology);
    			pelletReasoner.getKB().prepare();
    			List<InferredAxiomGenerator<? extends OWLAxiom>> generators = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
    	        generators.add(new InferredSubClassAxiomGenerator());
    	        generators.add(new InferredClassAssertionAxiomGenerator());
    	        generators.add(new InferredDisjointClassesAxiomGenerator());
    	        generators.add(new InferredEquivalentClassAxiomGenerator());
    	        generators.add(new InferredEquivalentDataPropertiesAxiomGenerator());
    	        generators.add(new InferredEquivalentObjectPropertyAxiomGenerator());
    	        generators.add(new InferredInverseObjectPropertiesAxiomGenerator());
    	        generators.add(new InferredPropertyAssertionGenerator());
    	        generators.add(new InferredSubDataPropertyAxiomGenerator());
    	        generators.add(new InferredSubObjectPropertyAxiomGenerator());
    	        
    	        InferredOntologyGenerator iog = new InferredOntologyGenerator(pelletReasoner, generators);
    			
    			OWLOntologyID id = ontology.getOntologyID();
    			Set<OWLImportsDeclaration> declarations = ontology.getImportsDeclarations();
    	        Set<OWLAnnotation> annotations = ontology.getAnnotations();
    	        
    	        Map<OWLEntity, Set<OWLAnnotationAssertionAxiom>> entityAnnotations = new HashMap<OWLEntity,Set<OWLAnnotationAssertionAxiom>>();
    	        for (OWLClass aEntity  : ontology.getClassesInSignature()) {
    	        	entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
    	        }
    	        for (OWLObjectProperty aEntity : ontology.getObjectPropertiesInSignature()) {
    	        	entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
    	        }
    	        for (OWLDataProperty aEntity : ontology.getDataPropertiesInSignature()) {
    	        	entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
    	        }
    	        for (OWLNamedIndividual aEntity : ontology.getIndividualsInSignature()) {
    	        	entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
    	        }
    	        for (OWLAnnotationProperty aEntity : ontology.getAnnotationPropertiesInSignature()) {
    	        	entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
    	        }
    	        for (OWLDatatype aEntity : ontology.getDatatypesInSignature()) {
    	        	entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
    	        }
    	        
    	        manager.removeOntology(ontology);
    	        OWLOntology inferred = manager.createOntology(id);
    			iog.fillOntology(manager, inferred);
    			
    			for (OWLImportsDeclaration decl : declarations) {
    	        	manager.applyChange(new AddImport(inferred, decl));
    	        }
    	        for (OWLAnnotation ann : annotations) {
    	        	manager.applyChange(new AddOntologyAnnotation(inferred, ann));
    	        }
    	        for (OWLClass aEntity : inferred.getClassesInSignature()) {
    	        	applyAnnotations(aEntity, entityAnnotations, manager, inferred);
    	        }
    	        for (OWLObjectProperty aEntity : inferred.getObjectPropertiesInSignature()) {
    	        	applyAnnotations(aEntity, entityAnnotations, manager, inferred);
    	        }
    	        for (OWLDataProperty aEntity : inferred.getDataPropertiesInSignature()) {
    	        	applyAnnotations(aEntity, entityAnnotations, manager, inferred);
    	        }
    	        for (OWLNamedIndividual aEntity : inferred.getIndividualsInSignature()) {
    	        	applyAnnotations(aEntity, entityAnnotations, manager, inferred);
    	        }
    	        for (OWLAnnotationProperty aEntity : inferred.getAnnotationPropertiesInSignature()) {
    	        	applyAnnotations(aEntity, entityAnnotations, manager, inferred);
    	        }
    	        for (OWLDatatype aEntity : inferred.getDatatypesInSignature()) {
    	        	applyAnnotations(aEntity, entityAnnotations, manager, inferred);
    	        }
    	        
				manager.saveOntology(inferred, new RDFXMLOntologyFormat(), parsedOntology);
        	}
        	else {
				manager.saveOntology(ontology, new RDFXMLOntologyFormat(), parsedOntology);
        	}
        	return parsedOntology.toString();
		}
		catch (Exception ex){
			throw new LodeException(ex);
		}
	}

	/**
	 * Generates an HTML-formatted description for an ontology definition.
	 * @param ontologyContent	The text of the ontology definition.
	 * @param ontologyUrl		The canonical URL (IRI) from which the ontology definition is/will be accessible.
	 * @param sourceUrl			The URL at which the plain-text source of the ontology definition can be accessed from.
	 * 							If this is null or empty, then the ontologyUrl will be appended to the sourceBase class property.
	 * @param lang				The code for the language which should be used for the ontology description.
	 * 							If this is null or empty, then English will be used.
	 * @return
	 * @throws LodeException
	 */
	public String transformOntology(
		String ontologyContent, 
		String ontologyUrl, 
		String sourceUrl, 
		String lang
	) throws LodeException {
		try {
			// Validate Parameters.
			UriHelper.validateURI(ontologyUrl, UriHelper.UriPathType.WebAbsolute, false, "Invalid ontologyUrl Parameter");
			String useSource = UriHelper.validatePath(sourceUrl, UriHelper.UriPathType.WebAny, true, "Invalid sourceUrl Parameter");
			if (useSource.isEmpty()){
				useSource = this.sourceBase + ontologyUrl;
			}
			String useLang = null;
			if (lang == null || lang.isEmpty()){
				useLang = this.languages[0];
			}
			else {
				for (String okLang : this.languages){
					if (okLang.equalsIgnoreCase(lang)){
						useLang = okLang;
						break;
					}
				}
				if (useLang == null){
					throw new IllegalArgumentException("Invalid lang parameter '" + lang + "'. Supported language codes are " + String.join(", ", this.languages));
				}
			}
					
			// Load and parse the XSLT.
			StreamSource xsltSource = new StreamSource(xsltPath);
			TransformerFactory tfactory = new net.sf.saxon.TransformerFactoryImpl();
			Transformer transformer = tfactory.newTransformer(xsltSource);
			transformer.setParameter("css-location", this.cssBase);
			transformer.setParameter("lang", useLang);
			transformer.setParameter("lode-home", this.lodeHome);
			transformer.setParameter("ontology-url", ontologyUrl);
			transformer.setParameter("source", useSource);
			transformer.setParameter("vendor-name", this.vendorName);
			transformer.setParameter("vendor-url", this.vendorUrl);
			transformer.setParameter("vis-base", this.visBase);
			
			// Use the XSLT to transform the Ontology Definition.
			String result = null;
			try (StringReader ontologyReader = new StringReader(ontologyContent)){
				try(ByteArrayOutputStream output = new ByteArrayOutputStream()){
					transformer.transform(new StreamSource(ontologyReader), new StreamResult(output));
					result = output.toString();
				}
			}
			return result;
		}
		catch(Exception ex){
			throw new LodeException (ex);					
		}
	}
	
	
	/**
	 * Generate an HTML document that displays information about an exception.
	 * @param e	The exception to be displayed. 
	 * @return	An HTML-formatted string containing information about e.
	 */
	public String getErrorHtml(Exception e) {
		String title = this.userAgent + " Error";
		return 
			"<html>" +
				"<head><title>" + title + "</title></head>" +
				"<body>" +
					"<h2>" +
					title +
					"</h2>" +
					"<p><strong>Reason: </strong>" +
					e.getMessage().replaceAll("\\n", "<br/>") +
					"</p>" +
				"</body>" +
			"</html>";
	}
}
