package org.launchermc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LaunchMinecraft {
    protected void launch(String username, String versionId, int allocatedMemory) {
        new Thread(() -> {
            try {
                // Путь к папке с версиями
                String versionFolderPath = "versions/" + versionId;
                File versionFolder = new File(versionFolderPath);

                if (!versionFolder.exists()) {
                    System.out.println("Версия не найдена. Сначала скачайте её.");
                    return;
                }

                // JAR файл клиента
                File clientJar = new File(versionFolder, versionId + "-client.jar");
                if (!clientJar.exists()) {
                    System.out.println("Файл клиента не найден: " + clientJar.getAbsolutePath());
                    return;
                }

                // Сборка classpath из всех библиотек
                File librariesFolder = new File(versionFolder, "libraries");
                StringBuilder classpath = new StringBuilder(clientJar.getAbsolutePath());
                if (librariesFolder.exists() && librariesFolder.isDirectory()) {
                    try {
                        Files.walk(Paths.get(librariesFolder.getAbsolutePath()))
                                .filter(Files::isRegularFile) // Только файлы
                                .filter(p -> p.toString().endsWith(".jar")) // Только файлы .jar
                                .forEach(p -> classpath.append(File.pathSeparator).append(p.toAbsolutePath()));
                    } catch (IOException e) {
                        System.out.println("Ошибка при обходе папки libraries: " + e.getMessage());
                    }
                } else {
                    System.out.println("Папка libraries не найдена или пуста.");
                }

                // Основной класс клиента Minecraft
                String mainClass = "net.minecraft.client.main.App";

                // Путь к папке ассетов
                File assetsFolder = new File("versions/assets");
                if (!assetsFolder.exists()) {
                    System.out.println("Папка с ассетами не найдена!");
                    return;
                }

                // Чтение файла JSON с метаданными версии
                File versionJsonFile = new File(versionFolder, versionId + ".json");
                if (!versionJsonFile.exists()) {
                    System.out.println("Файл метаданных версии не найден: " + versionJsonFile.getAbsolutePath());
                    return;
                }

                String assetIndex = null;
                try (BufferedReader reader = new BufferedReader(new FileReader(versionJsonFile))) {
                    JsonObject versionDetails = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject assetIndexObj = versionDetails.getAsJsonObject("assetIndex");
                    assetIndex = assetIndexObj.get("id").getAsString(); // Извлекаем assetIndex
                } catch (Exception e) {
                    System.out.println("Ошибка при чтении файла метаданных версии: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }

                if (assetIndex == null) {
                    System.out.println("assetIndex не найден в метаданных версии.");
                    return;
                }

                // Параметры запуска Minecraft
                ProcessBuilder processBuilder = new ProcessBuilder(
                        "java",
                        "-Xmx" + allocatedMemory + "M",
                        "-Xms" + allocatedMemory + "M",
                        "-cp", classpath.toString(),
                        mainClass,
                        "--username", username,
                        "--accessToken", "offline_token",
                        "--version", versionId,
                        "--assetsDir", assetsFolder.getAbsolutePath(),
                        "--assetIndex", assetIndex
                );

                processBuilder.directory(versionFolder);
                processBuilder.inheritIO();

                System.out.println("Запуск Minecraft с выделенной памятью: " + allocatedMemory + " МБ");
                Process process = processBuilder.start();

                int exitCode = process.waitFor();
                System.out.println("Minecraft завершился с кодом: " + exitCode);
            } catch (Exception e) {
                System.out.println("Ошибка запуска Minecraft: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}
