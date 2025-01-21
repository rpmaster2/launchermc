package org.launchermc;

import com.google.gson.JsonObject;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import java.util.ArrayList;
import java.util.List;

public class App extends Application {

    private ComboBox<String> versionSelector;
    private TextField usernameField;
    private ConfigManager configManager;
    private String username;
    private String version;
    private boolean showSnapshots;
    private boolean showBeta;
    private boolean showAlpha;
    private List<JsonObject> availableVersions = new ArrayList<>();

    // Переменные для отслеживания перемещения окна
    private double xOffset = 0;
    private double yOffset = 0;

    private static Logger logger;

    @Override
    public void start(Stage primaryStage) {
        // Инициализация логгера и перенаправление вывода System.out в лог-файл
        logger = new Logger("logs");
        System.out.println("Приложение запущено");
        // Создаем экземпляр ConfigManager для загрузки и сохранения конфигурации
        configManager = new ConfigManager();

        // Загружаем настройки
        showSnapshots = configManager.getBoolean("showSnapshots", true);
        showBeta = configManager.getBoolean("showBeta", true);
        showAlpha = configManager.getBoolean("showAlpha", true);


        // Поле ввода имени пользователя
        usernameField = new TextField();
        usernameField.setPromptText("Введите ваше имя");
        usernameField.setMaxWidth(200);
        usernameField.setStyle("-fx-font-size : 14pt;-fx-background-color: #5865F2;-fx-text-fill: white;");

        // Выбор версии
        versionSelector = new ComboBox<>();
        versionSelector.setPromptText("Выберите версию");
        versionSelector.setMaxWidth(200);
        versionSelector.setStyle(
                "-fx-font-size: 12pt; " +
                        "-fx-background-color: #5865F2; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;"
        );
        // Загрузка версий
        LoadAvailableVersion versionLoader = new LoadAvailableVersion();
        availableVersions.addAll(versionLoader.getAvailableVersions()); // Загружаем полную информацию
        updateVersionList(); // Обновляем список версий
        // Настройка стиля для выпадающих элементов
        versionSelector.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(
                            "-fx-background-color: #4752C4; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 12pt;"
                    );
                }

                // Добавляем стиль при наведении
                setOnMouseEntered(e -> setStyle("-fx-background-color: #5865F2; -fx-text-fill: white; -fx-font-size: 12pt;"));
                setOnMouseExited(e -> setStyle("-fx-background-color: #4752C4; -fx-text-fill: white; -fx-font-size: 12pt;"));
            }
        });
        String savedUsername = configManager.getUsername();
        System.out.println("Загруженное имя пользователя: " + savedUsername);
        if (savedUsername != null && !savedUsername.trim().isEmpty()) {
            usernameField.setText(savedUsername);
        }

        String savedVersion = configManager.getVersion();
        System.out.println("Загруженная версия: " + savedVersion);
        if (savedVersion != null && !savedVersion.trim().isEmpty()) {
            versionSelector.setValue(savedVersion);
        }



        // Кнопка 'Играть'
        Button playButton = new Button("Играть");
        playButton.setFont(Font.font(16));
        playButton.setOnAction(e -> {
            username = usernameField.getText();
            version = versionSelector.getValue();
            if (!username.isEmpty() && version != null) {
                // Сохраняем имя пользователя и выбранную версию

                configManager.save("username", username);
                configManager.save("version", version);

                // Запуск Minecraft
                LaunchMinecraft minecraft = new LaunchMinecraft();
                int allocatedMemory = configManager.getAllocatedMemory(); // Получаем выделенную память
                minecraft.launch(username, version, allocatedMemory);
                System.out.println("Запуск Minecraft с версией: " + version);
            } else {
                System.out.println("Введите имя пользователя и выберите версию.");
            }
        });

        // Кнопка 'Скачать версию'
        Button downloadButton = new Button("Скачать");
        downloadButton.setFont(Font.font(14));
        downloadButton.setOnAction(e -> {
            String version = versionSelector.getValue();
            if (version != null) {
                // Создаем экземпляр класса Download
                Download downloader = new Download();
                // Вызываем метод download с выбранной версией
                downloader.download(version);
                System.out.println("Скачивание версии: " + version);
            } else {
                System.out.println("Выберите версию для скачивания.");
            }
        });

        // Кастомный заголовок
        HBox titleBar = new HBox();
        Button minimizeButton = new Button("-");
        minimizeButton.setOnAction(event -> primaryStage.setIconified(true));
        minimizeButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-border-radius: 50; -fx-padding: 10px;");
        Button closeButton = new Button("X");
        closeButton.setOnAction(e -> {
            try {
                // Получаем текущие значения перед проверкой
                username = usernameField.getText();
                version = versionSelector.getValue();

                // Проверка на null и пустое значение
                if (username != null && !username.isEmpty()) {
                    configManager.save("username", username);
                } else {
                    System.out.println("Ошибка: username пустой или null");
                }

                if (version != null) {
                    configManager.save("version", version);
                } else {
                    System.out.println("Ошибка: version пустая или null");
                }

                System.out.println("Данные сохранены: username = " + username + ", version = " + version);
            } catch (Exception ex) {
                System.out.println("Произошла ошибка при сохранении данных: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                // Закрываем лаунчер
                System.out.println("Приложение закрывается");
                primaryStage.close();
                logger.close();
            }
        });



        closeButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-border-radius: 50; -fx-padding: 10px;");
        titleBar.getChildren().addAll(minimizeButton, closeButton);
        titleBar.setStyle("-fx-background-color: #282c34;-fx-border-radius: 10;-fx-border-color: white;-fx-padding: 10px;-fx-border-width: 3px");
        titleBar.setAlignment(Pos.CENTER_RIGHT);
        titleBar.setPadding(new Insets(10));
        titleBar.setSpacing(10);

        // Добавляем обработчик для перетаскивания окна
        titleBar.setOnMousePressed(this::handleMousePressed);
        titleBar.setOnMouseDragged(this::handleMouseDragged);

        // Контейнер для кнопок
        VBox controls = new VBox(20, usernameField, versionSelector, playButton, downloadButton);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(5));
        controls.setStyle("-fx-background-color: #282c34; -fx-border-radius: 20px;");

        // Стиль кнопок
        for (Button button : new Button[]{playButton, downloadButton}) {
            button.setTextFill(Color.WHITE);
            button.setStyle("-fx-background-color: #5865F2; -fx-background-radius: 5; -fx-cursor: hand;");
            button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #4752C4; -fx-background-radius: 5; -fx-cursor: hand;"));
        }

        // Кнопка настроек (иконка)
        ImageView settingsIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/settings.png")));
        settingsIcon.setFitWidth(50); // Увеличен размер иконки
        settingsIcon.setFitHeight(50); // Увеличен размер иконки
        settingsIcon.setPickOnBounds(true);
        settingsIcon.setOnMouseClicked(e -> openSettingsWindow());

        // Основной контейнер
        BorderPane root = new BorderPane();
        root.setTop(titleBar);  // Заголовок
        root.setCenter(controls);  // Контролы (поля ввода и кнопки)
        root.setBottom(settingsIcon); // Иконка настроек
        root.setStyle("-fx-background-color: #282c34;-fx-border-radius: 10;-fx-border-color: white;-fx-padding: 10px;-fx-border-width: 3px");

        // Сцена
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Minecraft Launcher");
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // Отключаем изменение размеров окна

        // Показываем окно
        primaryStage.show();
    }

    private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void handleMouseDragged(MouseEvent event) {
        // Получаем Stage, кастуя source к Stage
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    private void openSettingsWindow() {
        Stage settingsStage = new Stage();
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.setTitle("Настройки");

        // Логирование открытия окна настроек
        System.out.println("Открытие окна");
        // Получаем сохранённое значение памяти
        int allocatedMemory = configManager.getAllocatedMemory(); // Получаем выделенную память из конфига

        // CheckBox для фильтров
        CheckBox snapshotCheck = new CheckBox("Показывать снапшоты");
        snapshotCheck.setSelected(showSnapshots);
        snapshotCheck.setOnAction(e -> {
            showSnapshots = snapshotCheck.isSelected();
            configManager.save("showSnapshots", showSnapshots);
            updateVersionList();
            System.out.println("Сохранён параметр showSnapshots: " + showSnapshots);
        });
        snapshotCheck.setStyle("-fx-background-color: #5865F2; -fx-background-radius: 5; -fx-cursor: hand;");

        CheckBox betaCheck = new CheckBox("Показывать бета-версии");
        betaCheck.setSelected(showBeta);
        betaCheck.setOnAction(e -> {
            showBeta = betaCheck.isSelected();
            configManager.save("showBeta", showBeta);
            updateVersionList();
            System.out.println("Сохранён параметр showBeta: " + showBeta);
        });
        betaCheck.setStyle("-fx-background-color: #5865F2; -fx-background-radius: 5; -fx-cursor: hand;");

        CheckBox alphaCheck = new CheckBox("Показывать альфа-версии");
        alphaCheck.setSelected(showAlpha);
        alphaCheck.setOnAction(e -> {
            showAlpha = alphaCheck.isSelected();
            configManager.save("showAlpha", showAlpha);
            updateVersionList();
            System.out.println("Сохранён параметр showAlpha: " + showAlpha);
        });
        alphaCheck.setStyle("-fx-background-color: #5865F2; -fx-background-radius: 5; -fx-cursor: hand;");

        // Слайдер для выделения памяти
        SystemInfo si = new SystemInfo();
        GlobalMemory memory = si.getHardware().getMemory();
        long totalMemory = memory.getTotal() / (1024 * 1024);
        Slider memorySlider = new Slider();
        memorySlider.setMin(512); // Минимальное значение 512 МБ
        memorySlider.setMax(totalMemory); // Максимум - доступная память
        memorySlider.setValue(allocatedMemory); // Устанавливаем сохранённое значение
        memorySlider.setShowTickLabels(true);
        memorySlider.setShowTickMarks(true);
        memorySlider.setMajorTickUnit(512);
        memorySlider.setBlockIncrement(256);

        Label memoryLabel = new Label("Выделено памяти: " + allocatedMemory + " МБ");
        memoryLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12pt;");
        memorySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            memoryLabel.setText("Выделено памяти: " + newVal.intValue() + " МБ");
        });

        // Сохранение настроек при закрытии окна
        Button saveButton = new Button("Сохранить");
        saveButton.setOnAction(e -> {
            configManager.saveAllocatedMemory((int) memorySlider.getValue());
            System.out.println("Сохранено значение памяти: " + (int) memorySlider.getValue() + " МБ");
            settingsStage.close();
        });

        VBox settingsRoot = new VBox(10, snapshotCheck, betaCheck, alphaCheck, memoryLabel, memorySlider, saveButton);
        settingsRoot.setPadding(new Insets(20));
        settingsRoot.setAlignment(Pos.CENTER);
        settingsRoot.setStyle("-fx-background-color: #282c34; -fx-border-radius: 20px;");

        Scene settingsScene = new Scene(settingsRoot, 350, 300);
        settingsStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/settings.png")));
        settingsStage.setScene(settingsScene);
        settingsStage.showAndWait();
    }



    private void updateVersionList() {
        versionSelector.getItems().clear();

        for (JsonObject versionObject : availableVersions) {
            String versionId = versionObject.get("id").getAsString();
            String versionType = versionObject.get("type").getAsString();

            boolean include = true;

            if (!showSnapshots && "snapshot".equalsIgnoreCase(versionType)) {
                include = false;
            }
            if (!showBeta && "old_beta".equalsIgnoreCase(versionType)) {
                include = false;
            }
            if (!showAlpha && "old_alpha".equalsIgnoreCase(versionType)) {
                include = false;
            }

            if (include) {
                versionSelector.getItems().add(versionId);
            }
        }

        System.out.println("Фильтрованные версии: " + versionSelector.getItems());

        if (versionSelector.getItems().isEmpty()) {
            versionSelector.setPromptText("Нет доступных версий");
        }
    }
    public static void main(String[] args) {
        launch();
    }

}
