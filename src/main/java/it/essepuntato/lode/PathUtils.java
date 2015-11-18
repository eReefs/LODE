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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * PathUtils Helper Class
 */
public class PathUtils {
	
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
			if (path.matches("^/") && defaultIsLocal){
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
    			map.put(new URL(entryParts[0]), PathUtils.getURI(entryParts[1], true));
    		}
    	}
    	return map;
    }
}
