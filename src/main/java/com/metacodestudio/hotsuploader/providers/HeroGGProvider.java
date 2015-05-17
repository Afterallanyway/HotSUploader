package com.metacodestudio.hotsuploader.providers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metacodestudio.hotsuploader.models.ReplayFile;
import com.metacodestudio.hotsuploader.models.Status;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

public class HeroGGProvider extends Provider {

    private static final String ACCESS_KEY_ID = "beta:anQA9aBp";
    private static final String ENCODING = "UTF-8";

    public HeroGGProvider() {
        super("Hero.GG");
    }

    @Override
    public Status upload(final ReplayFile replayFile) {
        HttpURLConnection connection = null;
        String uri = "http://upload.hero.gg/ajax/upload-replay";

        String boundary = String.format("----------%s", UUID.randomUUID().toString().replaceAll("-", ""));
        String contentType = "multipart/form-data; boundary=" + boundary;

        try {
            byte[] fileData = getFileData(replayFile, boundary);

            URL url = new URL(uri);

            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("User-Agent", "HeroGG");
            connection.setFixedLengthStreamingMode((long) fileData.length);
            connection.setRequestProperty("Authorization", "Basic " + new String(Base64.encodeBase64(ACCESS_KEY_ID.getBytes(ENCODING))));

            OutputStream requestStream = connection.getOutputStream();
            requestStream.write(fileData, 0, fileData.length);
            requestStream.close();

            byte[] b;
            try (InputStream responseStream = connection.getInputStream()) {
                b = new byte[responseStream.available()];
                responseStream.read(b);
            }

            String result = new String(b, Charset.forName(ENCODING));
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> resultMap = mapper.readValue(result, Map.class);
            Object status = resultMap.get("success");

            if (status != null && (Boolean) status) {
                return Status.UPLOADED;
            } else {
                return Status.EXCEPTION;
            }

        } catch (UnsupportedEncodingException | ProtocolException | MalformedURLException | JsonParseException | JsonMappingException e) {
            return Status.EXCEPTION;
        } catch (IOException e) {
            return Status.NEW;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

    }

    private byte[] getFileData(final ReplayFile replayFile, String boundary) throws IOException {

        String key = getContentString(boundary, "key", "Nothing goes here at the moment");
        String name = getContentString(boundary, "name", replayFile.getFile().getName());
        String file = getContentString(boundary, "file", replayFile.getFile());
        String closing = "\r\n--" + boundary + "--\r\n";
        String newLine = "\r\n";

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            byte[] fileContents = Files.readAllBytes(replayFile.getFile().toPath());

            stream.write(key.getBytes(ENCODING));
            stream.write(newLine.getBytes(ENCODING));
            stream.write(name.getBytes(ENCODING));
            stream.write(newLine.getBytes(ENCODING));
            stream.write(file.getBytes(ENCODING));
            stream.write(fileContents);
            stream.write(closing.getBytes(ENCODING));
            
            return stream.toByteArray();
        }
    }

    private String getContentString(String boundary, String key, String value) {
        Object[] params = new Object[]{boundary, key, value};

        return String.format("--%1$s\r\nContent-Disposition: form-data; name=\"%2$s\"\r\n\r\n%3$s", params);
    }

    private String getContentString(String boundary, String key, File value) {
        Object[] params = new Object[]{boundary, key, value.getName(), "application/x-www-form-urlencoded"};

        return String.format("--%1$s\r\nContent-Disposition: form-data; name=\"%2$s\"; filename=\"%3$s\"\r\nContent-Type: %4$s\r\n\r\n", params);
    }
}
