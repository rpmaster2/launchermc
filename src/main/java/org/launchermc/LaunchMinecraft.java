package org.launchermc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

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

                JsonObject versionDetails;
                String assetIndex = null;
                try (BufferedReader reader = new BufferedReader(new FileReader(versionJsonFile))) {
                    versionDetails = JsonParser.parseReader(reader).getAsJsonObject();
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

                // Формируем путь к нативным библиотекам
                File librariesFolder = new File(versionFolder, "libraries");
                String nativePath = null;
                if (librariesFolder.exists() && librariesFolder.isDirectory()) {
                    File nativeDir = new File(versionFolder, "natives");
                    nativeDir.mkdirs(); // Создаем папку для нативных библиотек
                    try {
                        Files.walk(Paths.get(librariesFolder.getAbsolutePath()))
                                .filter(Files::isRegularFile)
                                .filter(p -> p.toString().contains("natives") && p.toString().endsWith(".jar"))
                                .forEach(p -> {
                                    try {
                                        // Распаковываем нативные библиотеки с учетом вложенных путей
                                        try (JarFile jar = new JarFile(p.toFile())) {
                                            jar.entries().asIterator().forEachRemaining(entry -> {
                                                if (!entry.isDirectory() &&
                                                        (entry.getName().endsWith(".dll") ||
                                                                entry.getName().endsWith(".so") ||
                                                                entry.getName().endsWith(".dylib"))) {
                                                    try {
                                                        // Сохраняем структуру папок (например, org/lwjgl/liblwjgl.so)
                                                        File nativeFile = new File(nativeDir, entry.getName());
                                                        nativeFile.getParentFile().mkdirs();
                                                        Files.copy(jar.getInputStream(entry), nativeFile.toPath(),
                                                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                                        System.out.println("Распакована нативная библиотека: " + nativeFile.getAbsolutePath());
                                                    } catch (IOException e) {
                                                        System.out.println("Ошибка при распаковке нативной библиотеки " + entry.getName() + ": " + e.getMessage());
                                                    }
                                                }
                                            });
                                        }
                                    } catch (IOException e) {
                                        System.out.println("Ошибка при обработке нативной библиотеки: " + p + ", " + e.getMessage());
                                    }
                                });
                        nativePath = nativeDir.getAbsolutePath();
                    } catch (IOException e) {
                        System.out.println("Ошибка при обходе папки libraries: " + e.getMessage());
                    }
                } else {
                    System.out.println("Папка libraries не найдена или пуста.");
                }

                // Формируем classpath с учетом правил из JSON
                StringBuilder classpath = new StringBuilder(clientJar.getAbsolutePath());
                if (librariesFolder.exists() && librariesFolder.isDirectory()) {
                    JsonArray libraries = versionDetails.getAsJsonArray("libraries");
                    String os = System.getProperty("os.name").toLowerCase();
                    for (JsonElement libElement : libraries) {
                        JsonObject library = libElement.getAsJsonObject();
                        if (!isLibraryCompatible(library, os)) {
                            continue;
                        }
                        JsonObject downloads = library.getAsJsonObject("downloads");
                        if (downloads != null) {
                            // Проверяем artifact
                            JsonObject artifact = downloads.getAsJsonObject("artifact");
                            if (artifact != null) {
                                String path = artifact.get("path").getAsString();
                                File libFile = new File(librariesFolder, path);
                                if (libFile.exists()) {
                                    classpath.append(File.pathSeparator).append(libFile.getAbsolutePath());
                                }
                            }
                            // Проверяем classifiers для нативных библиотек
                            if (downloads.has("classifiers")) {
                                JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                                String classifierKey = getClassifierKey(os);
                                if (classifierKey != null && classifiers.has(classifierKey)) {
                                    JsonObject classifier = classifiers.getAsJsonObject(classifierKey);
                                    String path = classifier.get("path").getAsString();
                                    File libFile = new File(librariesFolder, path);
                                    if (libFile.exists()) {
                                        classpath.append(File.pathSeparator).append(libFile.getAbsolutePath());
                                    }
                                }
                            }
                        }
                    }
                    System.out.println("Сформирован classpath: " + classpath.toString());
                } else {
                    System.out.println("Папка libraries не найдена или пуста, используется только клиентский JAR.");
                }

                // Выбираем mainClass на основе версии
                String mainClass;
                if (isVersionBefore1_6(versionId)) {
                    mainClass = "net.minecraft.client.Minecraft";
                } else {
                    mainClass = "net.minecraft.client.main.Main";
                }
                System.out.println("Выбран mainClass: " + mainClass + " для версии: " + versionId);

                // Запуск с -cp
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

                if (nativePath != null) {
                    processBuilder.command().add(2, "-Djava.library.path=" + nativePath);
                }

                processBuilder.directory(versionFolder);
                processBuilder.inheritIO();

                System.out.println("Запуск Minecraft с версией: " + versionId);
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

    // Проверка, является ли версия старше 1.6
    private boolean isVersionBefore1_6(String versionId) {
        try {
            String[] parts = versionId.split("\\.");
            if (parts.length < 2) return true;
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            return major == 1 && minor < 6; // Версии 1.5.2 и старше
        } catch (NumberFormatException e) {
            System.out.println("Ошибка парсинга версии: " + versionId + ". Предполагается старая версия.");
            return true;
        }
    }


    // Проверка совместимости библиотеки с текущей ОС
    private boolean isLibraryCompatible(JsonObject library, String os) {
        if (!library.has("rules")) {
            return true; // Нет правил — библиотека совместима
        }

        JsonArray rules = library.getAsJsonArray("rules");
        boolean allow = false;
        for (JsonElement ruleElement : rules) {
            JsonObject rule = ruleElement.getAsJsonObject();
            String action = rule.get("action").getAsString();
            if (rule.has("os")) {
                JsonObject osRule = rule.getAsJsonObject("os");
                String osName = osRule.get("name").getAsString();
                if ((os.contains("win") && osName.equals("windows")) ||
                        (os.contains("linux") && osName.equals("linux")) ||
                        (os.contains("mac") && osName.equals("osx"))) {
                    allow = action.equals("allow");
                }
            } else {
                allow = action.equals("allow");
            }
        }
        return allow;
    }

    // Получение ключа классификатора для текущей ОС
    private String getClassifierKey(String os) {
        if (os.contains("win")) {
            return "natives-windows";
        } else if (os.contains("linux")) {
            return "natives-linux";
        } else if (os.contains("mac")) {
            return "natives-macos";
        }
        return null;
    }
}