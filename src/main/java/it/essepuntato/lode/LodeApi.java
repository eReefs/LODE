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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
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
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
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
 */
public class LodeApi {
	    
    public static String parseSimple(URI ontologyUri) throws IOException, URISyntaxException{
    	SourceExtractor extractor = new SourceExtractor();
        extractor.addMimeTypes(MimeType.mimeTypes);
    	String content = extractor.getContent(ontologyUri);
    	return content;
    }

	public static String parseWithOWLAPI(
		URI ontologyUri,
		Map<URL, URI> ontologyMap,
		boolean considerImportedOntologies, 
		boolean considerImportedClosure,
		URI pelletPropertiesUri 
	) throws URISyntaxException, OWLOntologyCreationException, OWLOntologyStorageException  {
				
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		if (ontologyMap != null){
			for (Map.Entry<URL,URI> mapEntry : ontologyMap.entrySet()){
				manager.addIRIMapper(new SimpleIRIMapper(IRI.create(mapEntry.getKey()), IRI.create(mapEntry.getValue())));
			}
		}
				
		IRI ontologyIri = IRI.create(ontologyUri);
		HttpURLConnection.setFollowRedirects(true);
		OWLOntology ontology = null;
					
		if (considerImportedClosure || considerImportedOntologies) {
			ontology = manager.loadOntology(ontologyIri);
			Set<OWLOntology> setOfImportedOntologies = new HashSet<OWLOntology>();
			if (considerImportedOntologies) {
				setOfImportedOntologies.addAll(ontology.getDirectImports());
			} else {
				setOfImportedOntologies.addAll(ontology.getImportsClosure());
			}
			for (OWLOntology importedOntology : setOfImportedOntologies) {
				manager.addAxioms(ontology, importedOntology.getAxioms());
			}
		} 
		else {
			IRIDocumentSource source = new IRIDocumentSource(ontologyIri);
			OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
			ontology = manager.loadOntologyFromOntologyDocument(source, config);
		}
		
		if (pelletPropertiesUri != null) {
			ontology = parseWithReasoner(pelletPropertiesUri, manager, ontology);
		}
			
		StringDocumentTarget parsedOntology = new StringDocumentTarget();
		manager.saveOntology(ontology, new RDFXMLOntologyFormat(), parsedOntology);
		String result = parsedOntology.toString();
		return result;
	}
	

	public static OWLOntology parseWithReasoner(
		URI pelletPropertiesUri,
		OWLOntologyManager manager, 
		OWLOntology ontology
	) {
		try {
			PelletOptions.load(pelletPropertiesUri.toURL());
			PelletReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner(ontology);
			reasoner.getKB().prepare();
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
	        
	        InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner, generators);
			
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
	        
			return inferred;
		} 
		catch (IOException | OWLOntologyCreationException e) {
			return ontology;
		}
	}

	public static void applyAnnotations(
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
	
	public static String transformOntology(
		String ontologyContent, 
		String ontologyUrl, 
		String ontologySourceUrl, 
		String cssLocation, 
		String lang,
		String xsltPath,
		String lodeBase) 
	throws TransformerException, IOException{
		
		// Validate Parameters.
		if (ontologyContent == null || ontologyContent.isEmpty()){
			throw new IllegalArgumentException("The ontologyContent parameter may not be null or empty: you must pass in something to transform!");
		}
		if (ontologyUrl == null || ontologyUrl.isEmpty()){
			throw new IllegalArgumentException("The ontologyUrl parameter may not be null or empty");
		}
		String useSource = (ontologySourceUrl == null || ontologySourceUrl.isEmpty()) ? ontologyUrl : ontologySourceUrl;
		String useCss = (cssLocation == null || cssLocation.isEmpty()) ? "./" : cssLocation;
		String useLang = (lang == null || lang.isEmpty()) ? "en" : lang; 
		
		// Load and parse the XSLT.
		StreamSource xsltSource = new StreamSource(xsltPath);
		TransformerFactory tfactory = new net.sf.saxon.TransformerFactoryImpl();
		Transformer transformer = tfactory.newTransformer(xsltSource);
		transformer.setParameter("css-location", useCss);
		transformer.setParameter("lang", useLang);
		transformer.setParameter("ontology-url", ontologyUrl);
		transformer.setParameter("lode-base", lodeBase);
		transformer.setParameter("source", useSource);
		
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
	
	public static URI getURI(String path, boolean defaultIsLocal) throws URISyntaxException {
		String uri = path.replaceAll("\\s", "%20");
		if (File.separatorChar != '/' ){
			// Windows.
			uri = uri.replace(File.separatorChar, '/');
			if (uri.matches("^[A-Z]:/.+")){
				// Definitely an absolute local Windows file path.
				uri = "file:///" + uri;
			}
			else if (uri.matches("^//.+")){
				// Definitely an absolute Windows UNC path.
				uri = "file://" + uri.replaceAll("^//", "");
			}
		}
		else {
			// Posix file system
			if (path.matches("^/.+") && defaultIsLocal){
				// Treat paths starting with '/' as absolute file paths, rather than app-relative URLs.
				uri = "file://" + uri;
			}
		}
		return new URI(uri);
	}
    public static boolean isLocalFile(URI uri) {
    	String scheme = uri.getScheme();
    	String host = uri.getHost();
    	return "file".equalsIgnoreCase(scheme) && (host == null || host.isEmpty());
    }
    
    public static Map<URL, URI> parseUriMap(String mapText) throws MalformedURLException, URISyntaxException{
    	Map<URL, URI> map = null;
    	if (mapText != null && !mapText.isEmpty()){
    		map = new HashMap<URL, URI>();
    		String[] mapEntries = mapText.split("\\||\\n");
    		for(String mapEntry : mapEntries){
    			String[] entryParts = mapEntry.split("=", 2);
    			map.put(new URL(entryParts[0]), getURI(entryParts[1], true));
    		}
    	}
    	return map;
    }
}
