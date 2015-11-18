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
	private int maxTentative = 3;
       
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
			// Derive resource file paths and URLs from the runtime environment.
			ServletContext context = getServletContext(); 
			String servletUrl = request.getRequestURL().toString().replaceAll("/\\w+$", "/");
			String cssLocation = servletUrl;
			String pelletPropertiesPath = context.getRealPath("pellet.properties");
			String xsltPath = context.getRealPath("extraction.xsl");
			boolean webOnly = new Boolean(context.getInitParameter("webOnly"));
			LodeApi api = new LodeApi(xsltPath, cssLocation, pelletPropertiesPath, webOnly);
			
			// Extract the parse-parameters from the request made to the servlet.
			String ontologyUrl = request.getParameter("url");
			String ontologyPath = request.getParameter("path");
			String ontologyImports = request.getParameter("imports");
			String module = request.getParameter("module");
			boolean owlapi = ("owlapi".equalsIgnoreCase(module)) ? true : new Boolean(request.getParameter("owlapi"));
			boolean imported = ("imported".equalsIgnoreCase(module)) ? true : new Boolean(request.getParameter("imported"));
			boolean closure = ("closure".equalsIgnoreCase(module)) ? true : new Boolean(request.getParameter("closure"));
			boolean reasoner = "reasoner".equals(request.getParameter("reasoner")) ? true : new Boolean(request.getParameter("reasoner"));
						
			// Make a number of attempts to parse the requested ontology.
			String ontologyDefinition = "";
			for (int i = 0; i < maxTentative; i++) {
				try {
					ontologyDefinition = api.parseOntology(ontologyUrl, ontologyPath, ontologyImports, owlapi, imported, closure, reasoner);
					break;	// Out of the retry loop.
				} 
				catch (Exception e) {
					if (i + 1 == maxTentative) {
						// The final permitted attempt has just failed.
						throw e;
					}
				}
			}
			
			// Transform the ontology.
			// Use the 'source' servlet to display the ontology source.
			// The 'standard' cssLocation may be overridden by a request parameter.
			String ontologySourceUrl = servletUrl + "source";
			String cssOverride = request.getParameter("cssLocation");	// null => use the default.
			String lang = request.getParameter("lang");	// null => 'en'.
			result = api.transformOntology(ontologyDefinition, ontologyUrl, ontologySourceUrl, cssOverride, lang);
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
