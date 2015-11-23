<%
	String servletBase = request.getRequestURL().toString().replaceAll("/[\\w|\\.]+$", "/");
	java.util.Properties prop = new java.util.Properties();
	prop.load(getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF"));
	String appVersion = prop.getProperty("Implementation-Version"); 
	String buildDate = prop.getProperty("Build-Date");
%>
<!DOCTYPE html>
<html>
    <head>
        <title>LODE - Live OWL Documentation Environment</title>
        <link rel="shortcut icon" href="client/favicon.ico" />
        <link href="client/owl.css" rel="stylesheet" type="text/css" />
		<link href="client/Primer.css" rel="stylesheet" type="text/css" />
		<link href="client/rec.css" rel="stylesheet" type="text/css" />
		<link href="client/extra.css" rel="stylesheet" type="text/css" />
        <meta charset="UTF-8" />
		<style type="text/css">
			h1 {
				float:left;
			}
			form {
				text-align:left; 
				margin-top:70px;
				margin-bottom:50px;
				margin-left:30%;
			}
			form div {
				line-height:170%;
			}
			.text , .modules label {
				margin-right:1em;
			}
			.reasoner {
				margin-right:4em;
			}
			.noWrap {
				white-space:nowrap;
			}
        </style>
    </head>
    <body>
    	<h1>
			<img alt="LODE logo" src="client/LODEBanner.png">
		</h1>
		<form enctype="multipart/form-data" action="<%= servletBase %>extract" method="get">
			<input type="hidden" name="caller" value="<%= servletBase %>" />
			<p>
				<input type="url" id="url" name="url" placeholder="Put the complete ontology URL here" size="60"/> 
			</p>
			<div>
				<span class="modules">
					<label>Modules:</label>
					<input type="radio" name="module" value="" checked="checked" /> <span class="text">None</span>
					<input type="radio" name="module" value="owlapi" /> <span class="text">OWLAPI</span>
					<span class="noWrap">
						<input type="radio" name="module" value="imported" /> <span class="text">OWLAPI + Imported</span>
					</span>
					<span class="noWrap">
						<input type="radio" name="module" value="closure" /> <span class="text">OWLAPI + Closure</span>
					</span>
				</span><br />
				<input type="checkbox" id="reasoner" name="reasoner" value="reasoner" /> <span class="reasoner">Use the reasoner</span>
				<span class="lang noWrap">
					<label for="number">Language</label>:
					<input id="number" type="text" name="lang" value="" placeholder="'en' if empty" />
				</span>
			</div>
			<p><input type="submit" value="Generate Documentation"></p>
		</form>
        <p>
            <em><a href="http://www.github.com/essepuntato/LODE" title="Project homepage hosted by GitHub"><strong>L</strong>ive <strong>O</strong>WL <strong>D</strong>ocumentation <strong>E</strong>nvironment</a></em> (<em>LODE</em>), version <%= appVersion %> dated <%= buildDate %>, is a service that automatically extracts classes, object properties, data properties, named individuals, annotation properties, general axioms and namespace declarations from an OWL and OWL2 ontology, and renders them as ordered lists, together with their textual definitions, in a human-readable HTML page designed for browsing and navigation by means of embedded links.
        </p>
        <p>
            This LODE service is an open source development, and can be freely used, as described in this document. It may be used in conjunction with content negotiation to display this human-readable version of an OWL ontology when the user accesses the ontology using a web browser, or alternatively to deliver the OWL ontology itself when the user accesses the ontology using an ontology editing tool such as <a href="http://protege.stanford.edu/">Protégé</a> and <a href="http://neon-toolkit.org">NeOn Toolkit</a>. An exemplar implementation of such content negotiation is given in the <em><a href="http://www.w3.org/TR/swbp-vocab-pub/">Best Practice Recipes for Publishing RDF Vocabularies</a></em> by using the <em>.htaccess</em> file:
        </p>
        <pre>
AddType application/rdf+xml .rdf

# Rewrite engine setup
RewriteEngine On
			
# Rewrite rule to serve HTML content
RewriteCond %{HTTP_ACCEPT} !application/rdf\+xml.*(text/html|application/xhtml\+xml)
RewriteCond %{HTTP_ACCEPT} text/html [OR]
RewriteCond %{HTTP_ACCEPT} application/xhtml\+xml [OR]
RewriteCond %{HTTP_USER_AGENT} ^Mozilla/.*
<strong>RewriteRule ^ontology$ <%= servletBase %>extract?owlapi=true&url=http://www.mydomain.com/ontology [R=303,L]</strong>

# Rewrite rule to serve RDF/XML content if requested
RewriteCond %{HTTP_ACCEPT} application/rdf\+xml
RewriteRule ^ontology$ ontology.owl [R=303]

# Choose the default response
RewriteRule ^ontology$ ontology.owl [R=303]
		</pre>
        <p>
            Depending on the parameters specified, the HTML page created by LODE takes into account both ontological axioms and annotations. The CSSs used for the resulting HTML page are based on the <a href="http://www.w3.org">W3C</a> CSSs for Recommendation documents.
        </p>
        <p>
            The following pseudo-URL describes the way to call the LODE service:
        </p>
        <blockquote>
            <p><%= servletBase %>extract?optional-parameter-key=<strong>optional-parameter-value</strong>&url=<strong>ontology-url</strong></p>
        </blockquote>
        <p>
            where:
        </p>
        <dl>
            <dt><%= servletBase %>extract</dt>
            <dd>
                <p>
                    is the URL to call the service.
                </p>
            </dd>
            <dt>url</dt>
            <dd>
                <p>
                    is the full http://... URL of the OWL ontology that will be processed by the service. This is referred to as <strong>ontology-url</strong> in this documentation.
                </p>
            </dd>
        </dl>
        <p>optional request <strong>parameters</strong>:</p>
        <dl>
            <dt>owlapi</dt>
            <dd>
                <p>
                    When the value of this optional parameter is <strong>true</strong>, the ontology specified in <em>ontology-url</em> will be pre-processed via <a href="http://owlapi.sourceforge.net/">OWLAPI</a>, in order to linearized it in standard RDF/XML format. This parameter is always strongly recommended.
                </p>
            </dd>
            <dt>closure</dt>
            <dd>
                <p>
                    When the value of this optional parameter is <strong>true</strong>, the transitive closure given by considering the imported ontologies of <em>ontology-url</em> is added to the HTML description of the ontology. This parameter implicitly specifies the <em>owlapi</em> parameter. If both functions <em>closure</em> and <em>imported</em> are specified (in any order), just <em>imported</em> will be considered.
                </p>
            </dd>
            <dt>imported</dt>
            <dd>
                <p>
                    When the value of this optional parameter is <strong>true</strong>, the axioms contained the ontologies directed imported into <em>ontology-url</em> are added to the HTML description of the ontology. This parameter implicitly specifies the <em>owlapi</em> parameter. If both parameters <em>closure</em> and <em>imported</em> are specified (in any order), just <em>imported</em> will be considered.
                </p>
            </dd>
            <dt>reasoner</dt>
            <dd>
                <p>
                    When the value of this optional parameter is <strong>true</strong>, the assertions inferable from <em>ontology-url</em> using the <a href="http://clarkparsia.com/pellet">Pellet reasoner</a> will be added to the HTML description of the ontology. This parameter implicitly specifies the <em>owlapi</em> parameter. Note that, depending upon the nature of your ontology, this computationally intensive function can be very time-consuming.
                </p>
            </dd>
            <dt>lang=XXX</dt>
            <dd>
                <p>
                    When this optional parameter is specified, the specified language will be used as preferred language instead of English when showing annotations of the ontology specified in <em>ontology-url</em>. E.g.: <q>lang=it</q>, <q>lang=fr</q>, etc.
                </p>
            </dd>
        </dl>
        <h2>
            Some examples
        </h2>
        <ul>
            <li>
                <p>
                    <a href="<%= servletBase %>extract?url=http://purl.org/spar/fabio"><%= servletBase %>extract?url=http://purl.org/spar/fabio</a>
                </p>
            </li>
            <li>
                <p>
                    <a href="<%= servletBase %>extract?owlapi=true&url=http://www.essepuntato.it/2008/12/earmark"><%= servletBase %>extract?owlapi=true&url=http://www.essepuntato.it/2008/12/earmark</a>
                </p>
            </li>
            <li>
                <p>
                    <a href="<%= servletBase %>extract?imported=true&url=http://www.essepuntato.it/2008/12/earmark"><%= servletBase %>extract?imported=true&url=http://www.essepuntato.it/2008/12/earmark</a>
                </p>
            </li>
            <li>
                <p>
                    <a href="<%= servletBase %>extract?closure=true&url=http://www.essepuntato.it/2008/12/earmark"><%= servletBase %>extract?closure=true&url=http://www.essepuntato.it/2008/12/earmark</a>
                </p>
            </li>
        </ul>
        
        <h2>Annotations</h2>
        <p>
            LODE takes the most common properties used for denoting annotations, such as <em>dc:title</em>, <em>dc:description</em>, <em>rdfs:label</em> and <em>rdfs:comment</em>. The translation in HTML of annotations made using those properties varies a little depending on whether the annotations describe the ontology itself, or its entities (classes, properties, instances).
        </p>
        <p>
            The annotation properties handled by LODE are:
        </p>
        <ul>
            <li>
                <p>
                    dc:contributor
                </p>
            </li>
            <li>
                <p>
                    dc:creator
                </p>
            </li>
            <li>
                <p>
                    dc:date
                </p>
            </li>
            <li>
                <p>
                    dc:description, used with a literal as object, if you want to add a textual description to the ontology, or with a resource as object, if you want to trasclude that resource (e.g., a picture) as description of an entity.
                </p>
            </li>
            <li>
                <p>
                    dc:publisher
                </p>
            </li>
            <li>
                <p>
                    dc:rights
                </p>
            </li>
            <li>
                <p>
                    dc:title
                </p>
            </li>
            <li>
                <p>
                    owl:backwardCompatibleWith
                </p>
            </li>
            <li>
                <p>
                    owl:incompatibleWith
                </p>
            </li>
            <li>
                <p>
                    owl:versionInfo
                </p>
            </li>
            <li>
                <p>
                    owl:versionIRI
                </p>
            </li>
            <li>
                <p>
                    rdfs:comment
                </p>
            </li>
            <li>
                <p>
                    rdfs:isDefinedBy
                </p>
            </li>
            <li>
                <p>
                    rdfs:label
                </p>
            </li>
        </ul>

        <h2>References</h2>
        <p>
            Peroni, S., Shotton, D., Vitali, F. (2012). The Live OWL Documentation Environment: a tool for the automatic generation of ontology documentation. To appear in Proceedings of the 18th International Conference on Knowledge Engineering and Knowledge Management (EKAW 2012). Available at <a href="http://speroni.web.cs.unibo.it/publications/peroni-2012-live-documentation-environment.pdf">http://speroni.web.cs.unibo.it/publications/peroni-2012-live-documentation-environment.pdf</a>
        </p>
        <p>
        	Peroni, S., Shotton, D., Vitali, F. (2013). Tools for the automatic generation of ontology documentation: a task-based evaluation. To appear in International Journal on Semantic Web and Information Systems, 9 (1). Available at <a href="http://speroni.web.cs.unibo.it/publications/peroni-2013-tools-automatic-generation.pdf">http://speroni.web.cs.unibo.it/publications/peroni-2013-tools-automatic-generation.pdf</a>
        </p>
        
        <h2>About the author</h2>
        <p>
            <a href="http://www.essepuntato.it/">Silvio Peroni</a> (Ph.D.) is a researcher in computer science at the University of Bologna.
        </p>
    </body>
</html>