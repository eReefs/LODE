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
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class LodeServlet
 */
public class LodeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private LodeApi lode;
	private int maxTentative = 3;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LodeServlet() {
		super();
	}

	/**
	 * @see HttpServlet#init()
	 */
    public void init() throws ServletException {
		try {
			this.lode = new LodeApi(super.getServletContext(), "LODE Transform Servlet");
		}
		catch (Exception ex){
			super.log("INIT Error in LODE Transform Servlet instance", ex);
			throw new ServletException(ex);
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		for (int i = 0; i < maxTentative; i++) {
			try {
				String ontologyUrl = request.getParameter("url");
				String content = "";

				String imports = request.getParameter("imports");
				String module = request.getParameter("module");
				boolean useOWLAPI = new Boolean(request.getParameter("owlapi"));
				boolean considerImportedOntologies = ("imported".equalsIgnoreCase(module)) ? true : new Boolean(request.getParameter("imported"));
				boolean considerImportedClosure = ("closure".equalsIgnoreCase(module)) ? true : new Boolean(request.getParameter("closure"));
				boolean useReasoner = "reasoner".equals(request.getParameter("reasoner")) ? true : new Boolean(request.getParameter("reasoner"));

				if (considerImportedOntologies || considerImportedClosure || useReasoner) {
					useOWLAPI = true;
				}

				String lang = request.getParameter("lang");
				if (lang == null) {
					lang = "en";
				}

				if (useOWLAPI) {
					content = this.lode.parseWithOWLAPI(ontologyUrl, imports, considerImportedOntologies,
							considerImportedClosure, useReasoner);
				} else {
					content = this.lode.getOntologySource(ontologyUrl);
				}

				content = this.lode.applyXSLTTransformation(content, ontologyUrl, null, lang);

				out.println(content);
				i = maxTentative;
			} catch (Exception e) {
				super.log("GET Error in LODE Source Servlet instance", e);
				if (i + 1 == maxTentative) {
					out.println(getErrorPage(e));
				}
			}
		}
	}

	private String getErrorPage(Exception e) {
		return "<html>" + "<head><title>LODE error</title></head>" + "<body>" + "<h2>" + "LODE error" + "</h2>"
				+ "<p><strong>Reason: </strong>" + Encode.forHtml(e.getMessage()) + "</p>" + "</body>" + "</html>";
	}
}
