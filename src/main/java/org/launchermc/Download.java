package org.launchermc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Download {
    protected void download(String versionId) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());  // Пул потоков
        try {
            // Создание директории для версии
            File versionDir = new File("versions/" + versionId);
            if (!versionDir.exists()) {
                versionDir.mkdirs();
            }

            // Шаг 1: Получаем метаданные версии
            URL url = new URL("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray versions = jsonResponse.getAsJsonArray("versions");

            String versionUrl = null;
            for (int i = 0; i < versions.size(); i++) {
                JsonObject versionObject = versions.get(i).getAsJsonObject();
                if (versionObject.get("id").getAsString().equals(versionId)) {
                    versionUrl = versionObject.get("url").getAsString();
                    break;
                }
            }

            if (versionUrl == null) {
                System.out.println("Версия не найдена: " + versionId);
                return;
            }

            // Шаг 2: Загружаем JSON-файл версии
            System.out.println("Скачивание данных версии с URL: " + versionUrl);
            HttpURLConnection versionConnection = (HttpURLConnection) new URL(versionUrl).openConnection();
            versionConnection.setRequestMethod("GET");

            BufferedReader versionReader = new BufferedReader(new InputStreamReader(versionConnection.getInputStream()));
            StringBuilder versionResponse = new StringBuilder();
            while ((line = versionReader.readLine()) != null) {
                versionResponse.append(line);
            }
            versionReader.close();

            JsonObject versionDetails = JsonParser.parseString(versionResponse.toString()).getAsJsonObject();

            // Сохранение <versionId>.json
            File versionJsonFile = new File(versionDir, versionId + ".json");
            Files.write(versionJsonFile.toPath(), versionResponse.toString().getBytes());
            System.out.println("JSON-файл версии сохранен: " + versionJsonFile.getAbsolutePath());

            // Шаг 3: Скачиваем клиент JAR (параллельно)
            JsonObject downloads = versionDetails.getAsJsonObject("downloads");
            JsonObject clientDownload = downloads.getAsJsonObject("client");
            String clientUrl = clientDownload.get("url").getAsString();

            System.out.println("Скачивание клиента с URL: " + clientUrl);
            String clientFileName = versionId + "-client.jar";
            File clientFile = new File(versionDir, clientFileName);

            // Скачиваем клиент в отдельном потоке
            executor.submit(() -> {
                try {
                    Files.copy(new URL(clientUrl).openStream(), clientFile.toPath());
                    System.out.println("Версия клиента " + versionId + " успешно скачана.");
                } catch (IOException e) {
                    System.out.println("Ошибка при скачивании клиента: " + e.getMessage());
                }
            });

            // Шаг 4: Скачиваем библиотеки (параллельно)
            JsonArray libraries = versionDetails.getAsJsonArray("libraries");
            for (int i = 0; i < libraries.size(); i++) {
                JsonObject library = libraries.get(i).getAsJsonObject();
                JsonObject downloadsObj = library.getAsJsonObject("downloads");

                // Проверяем наличие artifact
                JsonObject artifact = downloadsObj != null ? downloadsObj.getAsJsonObject("artifact") : null;
                String libraryPath = null;
                String libraryUrl = null;

                if (artifact != null) {
                    libraryPath = artifact.get("path").getAsString();
                    libraryUrl = artifact.get("url").getAsString();
                } else if (downloadsObj != null && downloadsObj.has("classifiers")) {
                    // Обрабатываем classifiers для платформозависимых библиотек
                    JsonObject classifiers = downloadsObj.getAsJsonObject("classifiers");
                    String os = System.getProperty("os.name").toLowerCase();
                    String classifierKey = null;

                    if (os.contains("win")) {
                        classifierKey = "natives-windows";
                    } else if (os.contains("linux")) {
                        classifierKey = "natives-linux";
                    } else if (os.contains("mac")) {
                        classifierKey = "natives-macos";
                    }

                    if (classifierKey != null && classifiers.has(classifierKey)) {
                        JsonObject classifier = classifiers.getAsJsonObject(classifierKey);
                        libraryPath = classifier.get("path").getAsString();
                        libraryUrl = classifier.get("url").getAsString();
                    } else {
                        System.out.println("Пропущена библиотека без подходящего classifier для ОС: " + os + ", индекс: " + i);
                        System.out.println(library);
                        continue; // Пропускаем библиотеку, если нет подходящего classifier
                    }
                } else {
                    System.out.println("Пропущена библиотека без artifact и classifiers, индекс: " + i);
                    System.out.println(library);
                    continue; // Пропускаем библиотеку
                }

                // Создаем финальные копии для использования в лямбда-выражении
                final String finalLibraryUrl = libraryUrl;
                final File finalLibraryFile = new File(versionDir, "libraries/" + libraryPath);
                finalLibraryFile.getParentFile().mkdirs(); // Создаём директории для структуры

                System.out.println("Скачивание библиотеки с URL: " + finalLibraryUrl);

                // Скачиваем библиотеку в отдельном потоке
                executor.submit(() -> {
                    try {
                        Files.copy(new URL(finalLibraryUrl).openStream(), finalLibraryFile.toPath());
                        System.out.println("Библиотека успешно скачана: " + finalLibraryFile.getAbsolutePath());
                    } catch (IOException e) {
                        System.out.println("Ошибка при скачивании библиотеки: " + e.getMessage());
                    }
                });
            }

            // Шаг 5: Скачиваем ассеты (параллельно)
            downloadAssets(versionDetails, executor);

        } catch (Exception e) {
            System.out.println("Ошибка при скачивании версии: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();  // Завершаем пул потоков
        }
    }

    private void downloadAssets(JsonObject versionDetails, ExecutorService executor) throws IOException, InterruptedException {
        JsonObject assetIndex = versionDetails.getAsJsonObject("assetIndex");
        String assetIndexUrl = assetIndex.get("url").getAsString();
        String assetIndexName = assetIndex.get("id").getAsString();

        // Загружаем индекс ассетов
        System.out.println("Загрузка индекса ассетов с URL: " + assetIndexUrl);
        URL indexUrl = new URL(assetIndexUrl);
        HttpURLConnection indexConnection = (HttpURLConnection) indexUrl.openConnection();
        BufferedReader indexReader = new BufferedReader(new InputStreamReader(indexConnection.getInputStream()));
        StringBuilder indexResponse = new StringBuilder();
        String line;
        while ((line = indexReader.readLine()) != null) {
            indexResponse.append(line);
        }
        indexReader.close();

        // Сохраняем индекс ассетов
        File indexFile = new File("versions/assets/indexes/" + assetIndexName + ".json");
        indexFile.getParentFile().mkdirs();
        Files.write(indexFile.toPath(), indexResponse.toString().getBytes());
        System.out.println("Индекс ассетов сохранен: " + indexFile.getAbsolutePath());

        // Загружаем объекты ассетов
        JsonObject assetObjects = JsonParser.parseString(indexResponse.toString())
                .getAsJsonObject().getAsJsonObject("objects");

        for (String assetName : assetObjects.keySet()) {
            JsonObject asset = assetObjects.getAsJsonObject(assetName);
            String hash = asset.get("hash").getAsString();
            String hashPath = hash.substring(0, 2) + "/" + hash;

            // Формируем URL и путь для сохранения
            String assetUrl = "https://resources.download.minecraft.net/" + hashPath;
            File assetFile = new File("versions/assets/objects/" + hashPath);
            assetFile.getParentFile().mkdirs();

            // Скачивание ассета в отдельном потоке
            executor.submit(() -> {
                try {
                    if (!assetFile.exists()) {
                        System.out.println("Скачивание ассета: " + assetUrl);
                        Files.copy(new URL(assetUrl).openStream(), assetFile.toPath());
                        System.out.println("Ассет скачан: " + assetFile.getAbsolutePath());
                    } else {
                        System.out.println("Ассет уже существует: " + assetFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при скачивании ассета: " + assetUrl + ", " + e.getMessage());
                }
            });
        }
    }
}


