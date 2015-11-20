/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *      
 * Copyright (c) 2015, CSIRO
 * Author: Sharon Tickell <sharon.tickell@csiro.au>
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

import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.BasicParser;

public class LodeApp {

	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		// Set up the possible command-line arguments.
		Options options = new Options();
				
		Option ontologyHtmlPathOption = OptionBuilder.withArgName("ontologyHtmlPath")
													 .withDescription("Required: Absolute path to the local filesystem location where the HTML description of the ontology definition should be saved.")
													 .withLongOpt("ontologyHtmlPath")
													 .hasArg()
													 .isRequired()
													 .create("html");
		
		Option ontologyUrlOption = OptionBuilder.withArgName("ontologyUrl")
												.withDescription("Required: Absolute URL for the location where the ontology is (or will be) hosted.")
												.hasArg()
												.isRequired()
												.create("url");

		Option ontologyPathOption = OptionBuilder.withArgName("ontologyPath")
												 .withDescription("Optional: Absolute path to the ontology definition file on the local filesystem. If this is not provided, then the ontology definition will be read from the ontologyUrl.")
												 .hasArg()
												 .isRequired(false)
												 .create("path");
		
		Option ontologyImportsOption = OptionBuilder.withArgName("importUrl_1=importFilePath_1|...|importUrl_n=importFilePath_n")
													.withDescription("Optional: Mappings between URL and filesystem location for any ontologies imported by the ontology you wish to describe. Needed if those ontologies are not yet accessible at the URL in question. Formatted as url_1=file_1|url_2=file_2|...|url_n=file_n")
													.hasArg()
													.isRequired(false)
													.create("imports");
		
		Option ontologySourceOption = OptionBuilder.withArgName("ontologySource")
				 									.withDescription("Optional: Absolute URL for a visualisation of the ontology source. If not provided, then the application will attempt to derive this form the url parameter.")
				 									.hasArg()
				 									.isRequired(false)
				 									.create("source");
		
		Option languageOption = OptionBuilder.withArgName("languageCode")
											 .withDescription("Optional: The specified language will be used as preferred language instead of English when showing annotations of the ontology specified in ontology-url. E.g.: \"lang=it\", \"lang=fr\", etc.")
											 .hasArg()
											 .isRequired(false)
											 .create("lang");
		
        options.addOption(ontologyHtmlPathOption);
        options.addOption(ontologyUrlOption);
        options.addOption(ontologyPathOption);
        options.addOption(ontologyImportsOption);	        
        options.addOption("closure",  false, "Optional: When specified, the transitive closure given by considering the imported ontologies of ontology-url is added to the HTML description of the ontology. If both functions closure and imported are specified (in any order), just imported will be considered.");
        options.addOption("imported", false, "Optional: When specified, the axioms contained the ontologies directed imported into ontology-url are added to the HTML description of the ontology. If both parameters closure and imported are specified (in any order), just imported will be considered.");
        options.addOption("reasoner", false, "Optional: When specified, the assertions inferable from ontology-url using the Pellet reasoner will be added to the HTML description of the ontology. Note that, depending upon the nature of your ontology, this computationally intensive function can be very time-consuming.");
        options.addOption(ontologySourceOption);
        options.addOption(languageOption);

		try {
			// Parse the command line.
	        CommandLineParser parser = new BasicParser();
	        CommandLine cmd = parser.parse(options, args);
	        
	        // Retrieve and validate the source and destination locations for the ontology files.
	        String ontologyHtmlPath = cmd.getOptionValue(ontologyHtmlPathOption.getOpt());
	        URI ontologyHtmlUri = LodeApi.getURI(ontologyHtmlPath, true);
	        if (!LodeApi.isLocalFile(ontologyHtmlUri)){
	        	throw new IllegalArgumentException("Invalid ontologyHtmlPath value '" + ontologyHtmlUri.toString() + "'. This parameter must be a path to a local File.");
	        }
	        File ontologyHtmlFile = new File(ontologyHtmlUri);
	        if (ontologyHtmlFile.exists() && !ontologyHtmlFile.canWrite()){
	        	throw new IllegalArgumentException("Invalid ontologyHtmlPath value '" + ontologyHtmlUri.toString() + "'. A file already exists at that location that may not be overwritten.");	        	
	        }
	        
	        String ontologyUrl = cmd.getOptionValue(ontologyUrlOption.getOpt());
	        String ontologyPath = cmd.getOptionValue("path", null);
	        URI ontologyDocumentUri = (ontologyPath != null && !ontologyPath.isEmpty()) ?
	        							LodeApi.getURI(ontologyPath, true) :
	        							LodeApi.getURI(ontologyUrl, false);
	        
	        	        
			// Locate the directory that this .jar file is deployed to.
			URI executionUri = LodeApp.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			File executionFile = new File(executionUri);
			String contentDirPath = null;
			if (executionFile.isDirectory() && "classes".equals(executionFile.getName())){
				// Assume we are working from the source code structure in development
				// (The maven build output, which is at /target/classes)
				String baseDirPath = executionFile.getParentFile().getParent();
				contentDirPath = baseDirPath + File.separator + "src" + File.separator + "main" + File.separator + "webapp";
			}
			else {
				// ExecutionFile should be the .jar file, which should be directly in the contentDir.
				contentDirPath = executionFile.getParent();
			}
			String serverFilesPath = contentDirPath + File.separator + "server";
			String clientFilesPath = contentDirPath + File.separator + "client";
	        
	        // Parse the specified ontology document.
	        System.out.print("Parsing ontology definition document '" + ontologyDocumentUri.getPath() + "'... ");
	        String ontologyImports = cmd.getOptionValue("ontologyImports", null);
	        Map<URL, URI> ontologyImportsMap = LodeApi.parseUriMap(ontologyImports);
	        
	        boolean closure = cmd.hasOption("closure");
	        boolean imported = cmd.hasOption("imported");
	        boolean reasoner = cmd.hasOption("reasoner");
	        
	        URI pelletPropertiesUri = reasoner ? LodeApi.getURI(serverFilesPath + File.separator + "pellet.properties", true) : null;
	        
	        String ontologyContent = LodeApi.parseWithOWLAPI(ontologyDocumentUri, ontologyImportsMap, imported, closure, pelletPropertiesUri);
	        System.out.print("Parsed OK. \n\n");
	        
        	// Derive the URL and location of the source file from the HTML url and path.
        	String ontologySourceUrl = cmd.getOptionValue(ontologySourceOption.getOpt(), null);
        	if (ontologySourceUrl != null && !ontologySourceUrl.isEmpty()){
        		// A specific value for the source parameter was provided. 
        		System.out.println("Using provided source URL '" + ontologySourceUrl + "'.\n");       		
        	}
        	else {
        		// We will attempt to verify that the ontology source file lives in the same location
        		// that the ontology description file will be saved to, which allows it to be
        		// referenced from the HTML file via a relative path.
        		ontologySourceUrl = ontologyHtmlFile.getName().replaceAll("\\.\\w+$", "") + ".rdf";
        		String ontologySourcePath = ontologyHtmlFile.getParent() + File.separator + ontologySourceUrl;
        		        		
        		if (ontologySourcePath.equals(ontologyPath)){
        			System.out.println("Using '" + ontologySourceUrl + "' to reference the original, local ontology document at '" + ontologyPath + "'.\n");
        		}
        		else {
        			File ontologySourceFile = new File(ontologySourcePath);
        			if (ontologySourceFile.exists()){
        				System.out.println("WARNING: Unable to save and reference local ontology source, because the file '" + ontologySourcePath + "' already exists, but was not specified as the ontology path.\n");
        				ontologySourceUrl = null;
        			}
        			else {
        				System.out.print("\tSaving parsed ontology source to '" + ontologySourcePath + "'... ");
        				try (PrintWriter out = new PrintWriter(ontologySourceFile)){
        					out.write(ontologyContent);
        				}
        				System.out.print("OK\n"); 
        				System.out.println("Using '" + ontologySourceUrl + "' to reference the archived ontology source at '" + ontologySourcePath + "'.\n");
        			}	        	
        		}
        	}
				
	        // Transform the ontology Document to HTML, and save it to the specified location.
	        System.out.print("Transforming ontology definition to HTML... ");
	        String cssLocation = "lode/";
	        String xsltPath = serverFilesPath + File.separator + "extraction.xsl";
	        String lang = cmd.getOptionValue("lang", null);

	        String ontologyHtml = LodeApi.transformOntology(ontologyContent, ontologyUrl, ontologySourceUrl, cssLocation, lang, xsltPath);
	        System.out.print("Transformation succeeded.\n\n");
	        		
	        // Save the resulting document to the requested file.
	        System.out.print("Saving ontology description to '" + ontologyHtmlUri.getPath() + "'... ");
	        try (PrintWriter out = new PrintWriter(ontologyHtmlFile)){
	        	out.write(ontologyHtml);
	        }
	        System.out.print("Saved OK.\n\n");
	        
	        // Ensure the CSS and other resource files that the HTML file will need are also saved
	        // into the location relative to the HTML file that was just saved.
	        System.out.println("Ensuring css and image resource files exist:");
	        String clientFilesDestinationPath = ontologyHtmlFile.getParent() + File.separator + "lode";
	        File clientFilesDestination = new File(clientFilesDestinationPath);
	        clientFilesDestination.mkdirs();
	        
	        File clientFilesSource = new File(clientFilesPath);
	        File[] clientFiles = clientFilesSource.listFiles();
	        for (File sourceFile: clientFiles){
	        	String sourceFileName = sourceFile.getName();
	        	String destFileName = clientFilesDestinationPath + File.separator + sourceFileName;
	        	File destFile = new File(destFileName);
	        	if (destFile.exists()){
	        		System.out.println("\tClient resource file '" + destFile.getAbsolutePath() + "' already exists.");
	        	}
	        	else {
	        		System.out.print("\tSaving " + sourceFileName + " to '" + destFile.getAbsolutePath() + "'... ");
	        		Files.copy(sourceFile.toPath(), destFile.toPath());
	        		System.out.print("OK\n");
	        	}
	        }
	        System.out.println("LODE resource files saved successfully.");
	        System.out.println("\nDone.");
	    }
	    catch(org.apache.commons.cli.ParseException pe){
	        System.err.println("\nError attempting to parse the command line inputs: " + pe.getMessage());
	        (new HelpFormatter()).printHelp("LodeApp", options, true);
	    }
	    catch(Exception ex){
	        System.err.println("\nUnexpected error: " + ex.getMessage());            
	    }
	}

}
