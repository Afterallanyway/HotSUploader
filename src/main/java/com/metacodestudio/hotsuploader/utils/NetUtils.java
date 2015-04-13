package com.metacodestudio.hotsuploader.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class NetUtils {

    public static final String SPACE = "%20";

    private NetUtils() {
    }

    public static String simpleRequest(final String target) throws IOException {
        return simpleRequest(encode(target));
    }

    public static String simpleRequest(final URI uri) throws IOException {
        return simpleRequest(uri.toURL());
    }

    public static String simpleRequest(final URL url) throws IOException {
        InputStream inputStream = url.openStream();
        return IOUtils.readInputStream(inputStream);
    }

    /**
     * Encodes a given String url into a valid URI if possible
     * Method should be updated as required to make sure any valid String url that can be pasted into an address bar
     * will fit into a URI object
     * @param url the url to encode
     * @return a newly built URI if at all possible
     */
    public static URI encode(final String url) {
        return URI.create(url.replaceAll(" ", SPACE));
    }
}
