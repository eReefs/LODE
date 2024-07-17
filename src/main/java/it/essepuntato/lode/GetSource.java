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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class GetSource
 */
public class GetSource extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private LodeApi lode;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetSource() {
        super();
        // TODO Auto-generated constructor stub
    }
    
	/**
	 * @see HttpServlet#init()
	 */
    public void init() throws ServletException {
		try {
			this.lode = new LodeApi(super.getServletContext(), "LODE Source Servlet");
		}
		catch (Exception ex){
			super.log("INIT Error in LODE Source Servlet instance", ex);
			throw new ServletException(ex);
		}
    }
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String result = "";
		try {
			String url = request.getParameter("url");
			result = this.lode.getOntologySource(url);
			response.setContentType("text/plain");
		} 
		catch (Exception ex) {
			super.log("GET Error in LODE Source Servlet instance", ex);
			result = this.lode.getErrorHtml(ex);
			response.setContentType("text/html");
		}
		response.setCharacterEncoding("UTF-8");
		try (PrintWriter out = response.getWriter()){
			out.println(result);
		}
	}
}
