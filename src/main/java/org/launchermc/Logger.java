package org.launchermc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private File logFile;
    private PrintStream printStream;
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

            // Создаем PrintStream для перенаправления вывода
            printStream = new PrintStream(new FileOutputStream(logFile, true)) {
                @Override
                public void println(String x) {
                    String timestamp = dateFormat.format(new Date());
                    super.println(timestamp + " - " + x);
                }
            };

            // Перенаправляем стандартный вывод (System.out) в лог-файл
            System.setOut(printStream);

            // Инициализируем формат даты и времени для меток времени
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        } catch (IOException e) {
            System.out.println("Ошибка при создании лог-файла: " + e.getMessage());
        }
    }

    // Метод для закрытия PrintStream
    public void close() {
        if (printStream != null) {
            printStream.close();
        }
    }
}
