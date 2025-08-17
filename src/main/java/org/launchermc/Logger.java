package org.launchermc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private File logFile;
    private PrintStream filePrintStream;
    private PrintStream originalSystemOut;
    private SimpleDateFormat dateFormat;

    public Logger(String logDir) {
        try {
            // Создаем папку для логов, если она не существует
            File directory = new File(logDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Создаем лог-файл с уникальным именем на основе текущей даты и времени
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            logFile = new File(directory, "log_" + timestamp + ".txt");

            // Сохраняем оригинальный System.out
            originalSystemOut = System.out;

            // Создаем PrintStream для файла
            filePrintStream = new PrintStream(new FileOutputStream(logFile, true), true);

            // Создаем кастомный PrintStream, который пишет и в файл, и в консоль
            PrintStream dualPrintStream = new PrintStream(filePrintStream) {
                @Override
                public void println(String x) {
                    String timestampedMessage = dateFormat.format(new Date()) + " - " + x;
                    filePrintStream.println(timestampedMessage); // Пишем в файл с меткой времени
                    originalSystemOut.println(x); // Пишем в консоль без метки времени
                }

                @Override
                public void print(String x) {
                    filePrintStream.print(x); // Пишем в файл
                    originalSystemOut.print(x); // Пишем в консоль
                }

                // Переопределяем flush для синхронизации
                @Override
                public void flush() {
                    filePrintStream.flush();
                    originalSystemOut.flush();
                }
            };

            // Перенаправляем System.out на кастомный PrintStream
            System.setOut(dualPrintStream);

            // Инициализируем формат даты и времени для меток времени
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        } catch (IOException e) {
            // Используем originalSystemOut для вывода ошибки, если файл не создался
            if (originalSystemOut != null) {
                originalSystemOut.println("Ошибка при создании лог-файла: " + e.getMessage());
            } else {
                System.err.println("Ошибка при создании лог-файла: " + e.getMessage());
            }
        }
    }

    // Метод для закрытия PrintStream
    public void close() {
        if (filePrintStream != null) {
            filePrintStream.close();
        }
        // Восстанавливаем оригинальный System.out при закрытии
        if (originalSystemOut != null) {
            System.setOut(originalSystemOut);
        }
    }
}