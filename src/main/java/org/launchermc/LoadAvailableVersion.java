package org.launchermc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LoadAvailableVersion {

    private static final String VERSIONS_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String LOCAL_FILE = "version_manifest.json";

    protected List<JsonObject> getAvailableVersions() {
        List<JsonObject> versionList = new ArrayList<>();

        System.out.println("Начинается процесс загрузки доступных версий.");
        boolean isUpdated = downloadJson();

        if (isUpdated || Files.exists(Path.of(LOCAL_FILE))) {
            try (BufferedReader reader = new BufferedReader(new FileReader(LOCAL_FILE))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                JsonArray versions = jsonResponse.getAsJsonArray("versions");

                for (int i = 0; i < versions.size(); i++) {
                    JsonObject versionObject = versions.get(i).getAsJsonObject();
                    versionList.add(versionObject); // Сохраняем весь объект версии
                }
                System.out.println("Успешное чтение локального файла и загрузка версий.");
            } catch (IOException e) {
                System.err.println("Ошибка чтения локального файла: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Формат JSON поврежден: " + e.getMessage());
            }
        } else {
            System.err.println("Нет интернет-соединения и локального файла версий!");
        }

        return versionList;
    }


    private boolean downloadJson() {
        System.out.println("Начало загрузки JSON файла с URL: ");
        try {
            URL url = new URL(VERSIONS_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(LOCAL_FILE)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                System.out.println("JSON файл успешно скачан и сохранен локально.");
                return true;
            } else {
                System.err.println("Ошибка при загрузке JSON: " + connection.getResponseCode());
            }
        } catch (IOException e) {
            System.err.println("Ошибка соединения: " + e.getMessage());
        }

        return false;
    }
}
