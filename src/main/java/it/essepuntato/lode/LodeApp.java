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
import java.nio.file.Files;

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
		
		Option cssBaseOption = OptionBuilder.withArgName("cssBase")
				 .withDescription("Optional: the base URL *relative to where the HTML description will be saved* where stylesheets needed by the description should live. Defaults to 'lode'.")
				 .hasArg()
				 .isRequired(false)
				 .create("cssBase");

		Option sourceBaseOption = OptionBuilder.withArgName("sourceBase")
				 .withDescription("Optional: The base URL to which an ontology URL should be appended in order to reference the ontology definition source.")
				 .hasArg()
				 .isRequired(false)
				 .create("sourceBase");

		Option visBaseOption = OptionBuilder.withArgName("visBase")
				 .withDescription("Optional: The base URL to which an imported ontology URL should be appended for any 'Visualise it with Lode' links.")
				 .hasArg()
				 .isRequired(false)
				 .create("visBase");
		
        options.addOption(ontologyHtmlPathOption);
        options.addOption(ontologyUrlOption);
        options.addOption(ontologyPathOption);
        options.addOption(ontologyImportsOption);	        
        options.addOption("closure",  false, "Optional: When specified, the transitive closure given by considering the imported ontologies of ontology-url is added to the HTML description of the ontology. If both functions closure and imported are specified (in any order), just imported will be considered.");
        options.addOption("imported", false, "Optional: When specified, the axioms contained the ontologies directed imported into ontology-url are added to the HTML description of the ontology. If both parameters closure and imported are specified (in any order), just imported will be considered.");
        options.addOption("reasoner", false, "Optional: When specified, the assertions inferable from ontology-url using the Pellet reasoner will be added to the HTML description of the ontology. Note that, depending upon the nature of your ontology, this computationally intensive function can be very time-consuming.");
        options.addOption(ontologySourceOption);
        options.addOption("saveSource", false, "Optional: Whether a local RDF/XML copy of the ontology source should be saved in the same location as the HTML description. Ignored if an ontologySource value is provided.");
        options.addOption(languageOption);
        options.addOption(cssBaseOption);
        options.addOption(sourceBaseOption);
        options.addOption(visBaseOption);

		try {
			// Parse the command line.
	        CommandLineParser parser = new BasicParser();
	        CommandLine cmd = parser.parse(options, args);

			// Initialize  LODE Server instance.
			URI executionUri = LodeApp.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			File executionFile = new File(executionUri);
			String resourcePath = null;
			if (executionFile.isDirectory() && "classes".equals(executionFile.getName())){
				// Assume we are working from the source code structure in development
				// (The maven build output, which is at /target/classes)
				String baseDirPath = executionFile.getParentFile().getParent();
				resourcePath = baseDirPath + File.separator + "src" + File.separator + "main" + File.separator + "webapp";
			}
			else {
				// ExecutionFile should be the .jar file, which should be directly in the contentDir.
				resourcePath = executionFile.getParent();
			}

			String cssBase = cmd.getOptionValue("cssBase", "lode");
			String visBase = cmd.getOptionValue("visBase", null);
			LodeApi lode = new LodeApi(resourcePath, true, "LODE Command Line", cssBase, null, visBase);
			
	        // Retrieve and validate Ontology Description HTML save-location parameter.
	        String ontologyHtmlPath = cmd.getOptionValue(ontologyHtmlPathOption.getOpt());
	        URI ontologyHtmlUri = UriHelper.validateURI(ontologyHtmlPath, UriHelper.UriPathType.Filesystem, false, "Invalid HTML Path option");
	        File ontologyHtmlFile = new File(ontologyHtmlUri);
	        if (ontologyHtmlFile.exists() && !ontologyHtmlFile.canWrite()){
	        	throw new IllegalArgumentException("Invalid HTML Path Option '" + ontologyHtmlUri.toString() + "'. A file already exists at that location that may not be overwritten.");	        	
	        }
	        
	        // Parse the Ontology Definition.
	        String ontologyUrl = cmd.getOptionValue(ontologyUrlOption.getOpt());
	        String ontologyPath = cmd.getOptionValue("path", null);
	        String imports = cmd.getOptionValue("ontologyImports", null);
	        boolean closure = cmd.hasOption("closure");
	        boolean imported = cmd.hasOption("imported");
	        boolean reasoner = cmd.hasOption("reasoner");
	        
	        String ontologyDefinitionPath = (ontologyPath != null && !ontologyPath.isEmpty()) ? ontologyPath : ontologyUrl;
	        System.out.print("Parsing ontology definition document '" + ontologyDefinitionPath + "'... ");
	        String ontologyContent = lode.parseOntology(ontologyDefinitionPath, imports, imported, closure, reasoner);
	        System.out.print("Parsed OK. \n\n");
	        
        	// Work out what the source URL that will appear in the ontology description should be.
        	String ontologySourceUrl = cmd.getOptionValue(ontologySourceOption.getOpt(), null);
        	if (ontologySourceUrl != null && !ontologySourceUrl.isEmpty()){
        		// A specific value for the source URL was provided. Use it.
        		System.out.println("Using provided source URL '" + ontologySourceUrl + "'.\n");       		
        	}
        	else if (cmd.hasOption("saveSource")){
        		// Assume the definition source should exist as an RDF file in the same location
        		// as the ontology description, so we can use a relative URL to reference it.
        		ontologySourceUrl = ontologyHtmlFile.getName().replaceAll("\\.\\w+$", "") + ".rdf";
        		
        		// Do our best to ensure that the source file really does exist.
        		String ontologySourcePath = ontologyHtmlFile.getParent() + File.separator + ontologySourceUrl;      		
        		if (ontologySourcePath.equals(ontologyPath)){
        			// The calculated source location matches the definition we parsed.
        			System.out.println("Using '" + ontologySourceUrl + "' to reference the original, local ontology document at '" + ontologyPath + "'.\n");
        		}
        		else {
        			// We read the definition from elsewhere, and will need to save a copy
        			// to the location we are going to reference it from.
        			File ontologySourceFile = new File(ontologySourcePath);
        			if (ontologySourceFile.exists()){
        				System.out.println("WARNING: Unable to save and reference local ontology source, because the file '" + ontologySourcePath + "' already exists, but was not specified as the ontology path.\n");
        				ontologySourceUrl = null;
        			}
        			else {
        				System.out.print("\tSaving RDF/XML ontology source to '" + ontologySourcePath + "'... ");
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
	        String lang = cmd.getOptionValue("lang", null);
	        String ontologyHtml = lode.transformOntology(ontologyContent, ontologyUrl, ontologySourceUrl, lang);
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
	        String clientFilesDestinationPath = ontologyHtmlFile.getParent() + File.separator + cssBase;
	        File clientFilesDestination = new File(clientFilesDestinationPath);
	        clientFilesDestination.mkdirs();
	        
	        File clientFilesDir = new File(resourcePath + "/client");
	        File[] clientFiles = clientFilesDir.listFiles();
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
