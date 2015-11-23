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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class LodeServlet
 */
public class LodeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LodeServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(
			HttpServletRequest request, 
			HttpServletResponse response
	) throws ServletException, IOException {
		String result = "";
		try {
			// Derive context-dependent properties and resource file locations.
			ServletContext context = getServletContext(); 
			String servletUrl = request.getRequestURL().toString().replaceAll("/\\w+$", "/");
			String cssLocation = servletUrl + "client/";
			String pelletPropertiesPath = context.getRealPath("server/pellet.properties");
			String xsltPath = context.getRealPath("server/extraction.xsl");
						
			// Identify the URI for the Ontology definition that should be parsed,
			// and confirm that it is a remote URL, not the path to a local file.
			String ontologyUrl = request.getParameter("url");			
	    	URI ontologyDocumentUri = LodeApi.getURI(ontologyUrl, LodeApi.UriPathType.Remote); 			
	    		    	
	    	// Parse the ontology document using the requested modules.
	    	String module = request.getParameter("module");
			boolean owlapi = ("owlapi".equalsIgnoreCase(module)) ? true : new Boolean(request.getParameter("owlapi"));
			boolean imported = ("imported".equalsIgnoreCase(module)) ? true : new Boolean(request.getParameter("imported"));
			boolean closure = ("closure".equalsIgnoreCase(module)) ? true : new Boolean(request.getParameter("closure"));
			boolean reasoner = "reasoner".equals(request.getParameter("reasoner")) ? true : new Boolean(request.getParameter("reasoner"));
			
			String ontologyContent = "";
	        if (owlapi || imported || closure || reasoner) {
	        	// Imports may be mapped to remote URIs only. 
	        	// (Local file access would be a security risk).
	        	String ontologyImports = request.getParameter("imports");
	        	Map<URL, URI> ontologyImportsMap = LodeApi.parseUriMap(ontologyImports, LodeApi.UriPathType.Remote);
	        	
	        	URI pelletPropertiesUri = reasoner ? LodeApi.getURI(pelletPropertiesPath, LodeApi.UriPathType.Local) : null;
	        	ontologyContent = LodeApi.parseWithOWLAPI(ontologyDocumentUri, ontologyImportsMap, imported, closure, pelletPropertiesUri);
	        } 
	        else {
	        	ontologyContent = LodeApi.parseSimple(ontologyDocumentUri);
	        }
	        			
			// Transform the ontology.
			String ontologySourceUrl = servletUrl + "source?url=" + ontologyUrl;	// Calls the 'source' servlet in this webapp for this ontology definition.
			String lang = request.getParameter("lang");	// null => 'en'.
			String lodeBase = context.getInitParameter("lodeBase");
			if (lodeBase == null || lodeBase.isEmpty()){
				// No override for the lodeBase parameter has been provided. 
				// Use the current LODE instance, with the owlapi parameter turned on.
				lodeBase = servletUrl + "extract?owlapi=true&url=";
			}
			
			result = LodeApi.transformOntology(ontologyContent, ontologyUrl, ontologySourceUrl, cssLocation, lang, xsltPath, lodeBase);
		}
		catch (Exception e) {
			result = getErrorPage(e);
		}
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		try (PrintWriter out = response.getWriter()){
			out.println(result);
		}
	}
		
	private String getErrorPage(Exception e) {
		return 
			"<html>" +
				"<head><title>LODE error</title></head>" +
				"<body>" +
					"<h2>" +
					"LODE error" +
					"</h2>" +
					"<p><strong>Reason: </strong>" +
					e.getMessage().replaceAll("\\n", "<br/>") +
					"</p>" +
				"</body>" +
			"</html>";
	}

}
