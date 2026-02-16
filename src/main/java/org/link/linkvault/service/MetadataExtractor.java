package org.link.linkvault.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class MetadataExtractor {

    private static final int TIMEOUT_MS = 5000;

    public Map<String, String> extract(String url) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("title", "");
        metadata.put("description", "");
        metadata.put("favicon", buildDefaultFavicon(url));

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (LinkVault Bot)")
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            // Title: <title> or og:title
            String title = doc.title();
            Element ogTitle = doc.selectFirst("meta[property=og:title]");
            if (ogTitle != null && !ogTitle.attr("content").isEmpty()) {
                title = ogTitle.attr("content");
            }
            metadata.put("title", title);

            // Description: meta description or og:description
            String description = "";
            Element metaDesc = doc.selectFirst("meta[name=description]");
            if (metaDesc != null) {
                description = metaDesc.attr("content");
            }
            Element ogDesc = doc.selectFirst("meta[property=og:description]");
            if (ogDesc != null && !ogDesc.attr("content").isEmpty()) {
                description = ogDesc.attr("content");
            }
            metadata.put("description", truncate(description, 1000));

            // Favicon: <link rel="icon"> or default /favicon.ico
            Element iconLink = doc.selectFirst("link[rel~=(?i)(shortcut )?icon]");
            if (iconLink != null && !iconLink.attr("abs:href").isEmpty()) {
                metadata.put("favicon", iconLink.attr("abs:href"));
            }

        } catch (Exception e) {
            log.warn("Failed to extract metadata from {}: {}", url, e.getMessage());
        }

        return metadata;
    }

    private String buildDefaultFavicon(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() + "://" + uri.getHost() + "/favicon.ico";
        } catch (Exception e) {
            return "";
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
