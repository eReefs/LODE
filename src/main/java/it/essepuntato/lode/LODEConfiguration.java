package it.essepuntato.lode;

import java.nio.file.Path;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class LODEConfiguration {

	private static LODEConfiguration instance = null;

	private final String basePath;
	private String baseUrl = "/lode";
	private String defaultLang = "en";
	private int maxTentative = 3;
	private String vendorCss = "";
	private String vendorName = "";
	private String vendorUrl = "";
	private String webvowl = "http://visualdataweb.de/webvowl/#iri=";

	private LODEConfiguration(String basePath, String baseUrl) {
		if (basePath == null || basePath.isEmpty()){
			this.basePath = System.getProperty("user.dir");
		} else {
			this.basePath = basePath;
		}
		if (baseUrl != null && ! baseUrl.isEmpty()){
			this.baseUrl = baseUrl.trim().replaceAll("/$", "");
		}
		try {
			String configFilePath = System.getenv("LODE_CONFIG");
			if (configFilePath == null){
				configFilePath = Path.of(this.basePath, "config.properties").toString();
			}

			Configurations configs = new Configurations();
			Configuration config = configs.properties(configFilePath);

			String externalUrl = config.getString("externalURL", "").trim().replaceAll("/$", "");
			if (externalUrl != null && ! externalUrl.isEmpty()){
				this.baseUrl = externalUrl;
			}
			this.defaultLang = config.getString("defaultLang", this.defaultLang);
			this.maxTentative = config.getInt("maxTentative", this.maxTentative);
			this.vendorCss = config.getString("vendorCss", this.vendorCss);
			this.vendorName = config.getString("vendorName", this.vendorName);
			this.vendorUrl = config.getString("vendorUrl", this.webvowl);
			this.webvowl = config.getString("webvowl", this.webvowl);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public static LODEConfiguration getInstance(String basePath, String baseUrl) {
		if (instance == null) {
			instance = new LODEConfiguration(basePath, baseUrl);
		}
		return instance;
	}


	public String getBaseUrl() {
        return this.baseUrl;
    }

	public String getCssLocation() {
        return this.baseUrl + "/";
    }

	public String getDefaultLang() {
		return this.defaultLang;
	}

	public String getExtractUrl() {
        return this.baseUrl + "/extract?url=";
    }

	public int getMaxTentative() {
		return this.maxTentative;
	}

	public String getPelletPropertiesUrl() {
        return this.baseUrl + "/pellet.properties";
    }

	public String getSourceUrl() {
        return this.baseUrl + "/source?url=";
    }

	public String getVendorCss() {
		return this.vendorCss;
	}

	public String getVendorName() {
		return this.vendorName;
	}

	public String getVendorUrl() {
		return this.vendorUrl;
	}

	public String getWebvowl() {
		return this.webvowl;
	}

	public String getXsltPath(){
		return Path.of(this.basePath, "extraction.xsl").toAbsolutePath().toString();
	}
}
