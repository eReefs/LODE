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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SourceExtractor {
	private List<String> mimeTypes;
	private int maxRedirects = 50;

	public SourceExtractor() {
		mimeTypes = new ArrayList<String>();
	}
	public SourceExtractor(int maxRedirects) {
		this();
		this.maxRedirects = maxRedirects;
	}
	
	public void addMimeType(String mimeType){
		mimeTypes.add(mimeType);
	}
	
	public void addMimeTypes(String[] mimeTypes) {
		for (String mimeType : mimeTypes) {
			addMimeType(mimeType);
		}
	}
	
	public void removeMimeType(String mimeType){
		mimeTypes.remove(mimeType);
	}
	
	public InputStream getInputStream(URI uri) throws IOException, URISyntaxException {
		InputStream sourceStream = null;
		if ("file".equalsIgnoreCase(uri.getScheme())){
			sourceStream = new FileInputStream(new File(uri));
		}
		else {
			URL url = uri.toURL();
			HttpURLConnection.setFollowRedirects(false);
			StringBuilder errorMessage = new StringBuilder();
			for (String mimeType : this.mimeTypes) {
				try {
					HttpURLConnection connection = null;
					
					boolean redirect = true;
					int redirectCount = 0;
					while (redirect && sourceStream == null) {
						connection = (HttpURLConnection) url.openConnection();
						connection.setInstanceFollowRedirects(false);
						connection.setRequestProperty("User-Agent", "LODE extractor");
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
							if (redirectCount >= this.maxRedirects){
								// We're not *allowed* to follow any more.
								errorMessage.append("MIME type '" + mimeType + "': Too many redirects! Reached limit of " + this.maxRedirects + " with url '" + url.toString() + "'.\n");
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
	
	
	
	public String getContent(URI uri) throws IOException, URISyntaxException {
		StringBuilder content = new StringBuilder();
		try (InputStream sourceStream = getInputStream(uri)){
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
	
	public String exec(URL url) throws IOException, URISyntaxException {
		return getContent(url.toURI());
	}
}
