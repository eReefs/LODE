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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class UriHelper {
	
	public enum UriPathType {
		Any,
		Filesystem,
		WebAny,
		WebAbsolute,
	}
	
	public static URI getURI(String path, boolean defaultIsFilesystem) throws URISyntaxException {
		String realPath = path.replaceAll("\\s", "%20");
		if (File.separatorChar != '/' ){
			// Windows.
			realPath = realPath.replace(File.separatorChar, '/');
			if (realPath.matches("^[A-Z]:/.+")){
				// Definitely an absolute local Windows file path.
				realPath = "file:///" + realPath;
			}
			else if (realPath.matches("^//.+")){
				// Definitely an absolute Windows UNC path.
				realPath = "file://" + realPath.replaceAll("^//", "");
			}
		}
		else {
			// Posix file system
			if (path.matches("^/.+") && defaultIsFilesystem ){
				// Treat paths starting with '/' as absolute file paths, rather than app-relative URLs.
				realPath = "file://" + realPath;
			}
		}
		return new URI(realPath);
	}
	
	public static URI validateURI(String path, UriPathType pathType, boolean allowEmpty, String failMessage) throws IllegalArgumentException {
		String useMessage = (failMessage == null || failMessage.isEmpty()) ? "Invalid URI path: " : failMessage;
		if (path == null || path.isEmpty()){
			if (allowEmpty){
				return null;
			}
			else {
				throw new IllegalArgumentException(useMessage + ". Null or empty values are not permitted.");
			}
		}
		URI uri = null;
		try {
			uri = getURI(path, pathType == UriPathType.Filesystem);		
		}
		catch (URISyntaxException se){
			throw new IllegalArgumentException(useMessage + ". Not a valid URI.", se);
		}
		switch(pathType){
		case Filesystem:
			if (! "file".equalsIgnoreCase(uri.getScheme())){
				throw new IllegalArgumentException(useMessage + " '" + path.toString() + "'. Only local filesystem paths are permitted.");				
			}
			break;
		case WebAny:
			if ("file".equalsIgnoreCase(uri.getScheme())){
				throw new IllegalArgumentException(useMessage + " '" + path.toString() + "'. Local filesystem paths are not permitted.");
			}
			break;
		case WebAbsolute:
			String host = uri.getHost();
			if ("file".equalsIgnoreCase(uri.getScheme()) || ! uri.isAbsolute() || host == null || host.isEmpty()){
				throw new IllegalArgumentException(useMessage + " '" + path.toString() + "'. Only absolute/remote Web URLs are permitted.");
			}
			break;
		default:
			// Don't care what sort of URI is provided.
			break;
		}
		return uri;
	}
	
	public static String validatePath(String path, UriPathType pathType, boolean allowEmpty, String failMessage) throws IllegalArgumentException {
		URI uri = validateURI(path, pathType, allowEmpty, failMessage);
		if (uri == null){
			return "";
		}
		else {
			return uri.toString();
		}
	}
    
    public static Map<URL, URI> parseUriMap(String mapText, UriPathType pathType) throws MalformedURLException, IllegalArgumentException {
    	Map<URL, URI> map = null;
    	if (mapText != null && !mapText.isEmpty()){
    		map = new HashMap<URL, URI>();
    		String[] mapEntries = mapText.split("\\||\\n");
    		for(String mapEntry : mapEntries){
    			String[] entryParts = mapEntry.split("=", 2);
    			URI original = validateURI(entryParts[0], UriPathType.WebAbsolute, false, "Invalid Import URL to be Mapped");
    			URI replacement = validateURI(entryParts[1], pathType, false, "Invalid URI to be mapped to '" + original.toString() + "'");
    			map.put(original.toURL(), replacement);
    		}
    	}
    	return map;
    }
		
	public static InputStream getInputStream(URI uri, Iterable<String> mimeTypes, String userAgent, int maxRedirects) throws IOException, URISyntaxException {
		InputStream sourceStream = null;
		if ("file".equalsIgnoreCase(uri.getScheme())){
			sourceStream = new FileInputStream(new File(uri));
		}
		else {
			URL url = uri.toURL();
			HttpURLConnection.setFollowRedirects(false);
			StringBuilder errorMessage = new StringBuilder();
			for (String mimeType : mimeTypes) {
				try {
					HttpURLConnection connection = null;
					
					boolean redirect = true;
					int redirectCount = 0;
					while (redirect && sourceStream == null) {
						connection = (HttpURLConnection) url.openConnection();
						connection.setInstanceFollowRedirects(false);
						connection.setRequestProperty("User-Agent", userAgent);
						connection.setRequestProperty("Accept", mimeType);
						
						int status = connection.getResponseCode();
						if (status == HttpURLConnection.HTTP_OK) {
							// We have successfully retrieved the document using this MIME type.
							sourceStream = connection.getInputStream();
							redirect = false;
						}
						else if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
								 status == HttpURLConnection.HTTP_MOVED_PERM	|| 
								 status == HttpURLConnection.HTTP_SEE_OTHER) {
							// We need to follow the redirect to retrieve the document.
							if (redirectCount >= maxRedirects){
								// We're not *allowed* to follow any more.
								errorMessage.append("MIME type '" + mimeType + "': Too many redirects! Reached limit of " + maxRedirects + " with url '" + url.toString() + "'.\n");
								redirect = false;
							}
							else {
								url = new URL(connection.getHeaderField("Location"));
								redirectCount++;
								redirect = true;
							}
						}
						else {
							// We attempted an invalid URL.
							errorMessage.append("MIME type '" + mimeType + "': Received HTTP Response code " + status + " for URL '" + url.toString() + "' at redirect " + redirectCount + ".");
							redirect = false;
						}
					}
					if (sourceStream != null){
						// This latest MIME type was correct.
						break;	// Out of the for-loop 
					}					
				}
				catch (Exception e) {
					errorMessage.append("MIME type '" + mimeType + "': # " + e.getMessage() + "\n");
				}
			}
			if (sourceStream == null) {
				throw new IOException("The source can't be downloaded in any permitted format.\n" + errorMessage.toString());
			}
		}
		return sourceStream;
	}
	
	public static String getSource(URI uri, Iterable<String> mimeTypes, String userAgent, int maxRedirects) throws IOException, URISyntaxException {
		StringBuilder content = new StringBuilder();
		try (InputStream sourceStream = getInputStream(uri, mimeTypes, userAgent, maxRedirects)){
			try (InputStreamReader sourceStreamReader = new InputStreamReader(sourceStream)){
				try (BufferedReader sourceTextReader = new BufferedReader(sourceStreamReader)){
					String line;
					while ((line = sourceTextReader.readLine()) != null) {
						content.append(line);
						content.append("\n");
					}
				}
			}
		}
		return content.toString();
	}
}
