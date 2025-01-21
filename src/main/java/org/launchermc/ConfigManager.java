package org.launchermc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {

    private static final String CONFIG_FILE = "launcher_config.ini";
    private Properties properties;

    public ConfigManager() {
        properties = new Properties();
        load();
    }

    // Загрузить данные из конфигурационного файла
    public void load() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            System.out.println("Конфигурационный файл не найден. Создаем новый.");
            save("username", "");  // Дефолтное значение
            save("version", "");   // Дефолтное значение
            return;
        }
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        } catch (IOException e) {
            System.out.println("Ошибка при загрузке конфигурации: " + e.getMessage());
        }
    }



    // Сохранить строковое значение в конфигурационный файл
    public void save(String key, String value) {
        if (key != null || value != null) {
            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                properties.setProperty(key, value);
                properties.store(fos, null);
                System.out.println("Сохранено значение: " + key + " = " + value);
            } catch (IOException e) {
                System.out.println("Ошибка при сохранении конфигурации: " + e.getMessage());
            }
        }
        else {
            System.out.println("Ошибка: ключ или значение не могут быть пустыми");
        }
    }

    // Сохранить булевое значение в конфигурационный файл
    public void save(String key, boolean value) {
        save(key, Boolean.toString(value));
    }

    // Сохранить целочисленное значение в конфигурационный файл
    public void save(String key, int value) {
        save(key, Integer.toString(value));
    }

    // Получить строковое значение из конфигурации
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    // Получить булевое значение из конфигурации
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    // Получить целочисленное значение из конфигурации
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public String getUsername() {
        String username = getString("username", "");
        System.out.println("Загружено имя пользователя: " + username);
        return username;
    }

    public String getVersion() {
        String version = getString("version", "");
        System.out.println("Загружена версия: " + version);
        return version;
    }


    // Получить выделенную память
    public int getAllocatedMemory() {
        return getInt("allocatedMemory", 1024); // По умолчанию 1024 МБ
    }

    // Сохранить выделенную память
    public void saveAllocatedMemory(int memory) {
        save("allocatedMemory", memory);
    }
}
