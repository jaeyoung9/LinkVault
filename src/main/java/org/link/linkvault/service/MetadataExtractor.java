package org.link.linkvault.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class MetadataExtractor {

    private static final int TIMEOUT_MS = 5000;

    @Value("${app.security.ssrf.allow-local:true}")
    private boolean allowLocal;

    public Map<String, String> extract(String url) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("title", "");
        metadata.put("description", "");
        metadata.put("favicon", buildDefaultFavicon(url));

        try {
            validateUrl(url);

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

    private void validateUrl(String url) throws Exception {
        URI uri = new URI(url);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only http/https URLs are allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL must have a valid host");
        }

        if (!allowLocal) {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isAnyLocalAddress()
                    || address.isMulticastAddress()) {
                throw new IllegalArgumentException("Requests to internal addresses are not allowed");
            }

            String hostLower = host.toLowerCase();
            if (hostLower.equals("localhost") || hostLower.endsWith(".local")
                    || hostLower.equals("169.254.169.254") || hostLower.equals("[::1]")) {
                throw new IllegalArgumentException("Requests to internal addresses are not allowed");
            }
        }
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
