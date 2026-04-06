package org.example.Lab2;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public class HelloController {

    @FXML
    private TextArea textArea;

    @FXML
    private TextArea textArea2;

    @FXML
    private TableView<Token> resultTable;

    @FXML
    private TableColumn<Token, Integer> codeCol;

    @FXML
    private TableColumn<Token, String> typeCol;

    @FXML
    private TableColumn<Token, String> lexemeCol;

    @FXML
    private TableColumn<Token, String> locationCol;

    @FXML
    private TableView<SyntaxError> syntaxErrorTable;

    @FXML
    private TableColumn<SyntaxError, String> errorFragmentCol;

    @FXML
    private TableColumn<SyntaxError, String> errorLocationCol;

    @FXML
    private TableColumn<SyntaxError, String> errorDescCol;

    private ObservableList<Token> tokenData = FXCollections.observableArrayList();
    private ObservableList<SyntaxError> syntaxErrorData = FXCollections.observableArrayList();

    private File currentFile;
    private double fontSize = 14;
    private boolean isModified = false;

    @FXML
    public void initialize() {

        textArea2.setEditable(false);
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        lexemeCol.setCellValueFactory(new PropertyValueFactory<>("text"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        
        resultTable.setItems(tokenData);

        resultTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null && newSel.getCode() == 17) {
                textArea.requestFocus();
                textArea.selectRange(newSel.getGlobalStart(), newSel.getGlobalEnd());
            }
        });
        errorFragmentCol.setCellValueFactory(new PropertyValueFactory<>("fragment"));
        errorLocationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        errorDescCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        syntaxErrorTable.setItems(syntaxErrorData);

        syntaxErrorTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                textArea.requestFocus();
                textArea.selectRange(newSel.getGlobalStart(), newSel.getGlobalEnd());
            }
        });

        textArea.textProperty().addListener((obs, oldText, newText) -> {
            isModified = true;
            updateTitle();
        });

        textArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {

                Stage stage = (Stage) newScene.getWindow();

                stage.setOnCloseRequest(event -> {
                    if (!checkSaveBeforeAction()) {
                        event.consume();
                    }
                });

                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
                        this::handleSave
                );

                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                        this::handleNew
                );

                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN),
                        this::handleOpen
                );
            }
        });
    }

    @FXML
    private void increaseFont() {
        fontSize += 2;
        updateFont();
    }

    @FXML
    private void decreaseFont() {
        if (fontSize > 8) {
            fontSize -= 2;
            updateFont();
        }
    }

    private void updateFont() {
        textArea.setStyle("-fx-font-size: " + fontSize + "px;");
        textArea2.setStyle("-fx-font-size: " + fontSize + "px;");
    }

    private void updateTitle() {
        Stage stage = (Stage) textArea.getScene().getWindow();

        String fileName = (currentFile == null)
                ? "Без имени"
                : currentFile.getName();

        String modifiedMark = isModified ? " *" : "";

        stage.setTitle("Текстовый редактор - " + fileName + modifiedMark);
    }

    private boolean checkSaveBeforeAction() {

        if (!isModified) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Несохранённые изменения");
        alert.setHeaderText("Файл был изменён");
        alert.setContentText("Сохранить изменения перед продолжением?");

        ButtonType saveBtn = new ButtonType("Сохранить");
        ButtonType dontSaveBtn = new ButtonType("Не сохранять");
        ButtonType cancelBtn = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveBtn, dontSaveBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {

            if (result.get() == saveBtn) {
                handleSave();
                return !isModified;
            }
            else if (result.get() == dontSaveBtn) {
                return true;
            }
        }

        return false;
    }


    @FXML
    private void handleNew() {
        if (!checkSaveBeforeAction()) return;

        textArea.clear();
        currentFile = null;
        isModified = false;
        updateTitle();
        textArea2.appendText("Создан новый документ\n");
    }

    @FXML
    private void handleOpen() {
        if (!checkSaveBeforeAction()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Открыть файл");

        File file = fileChooser.showOpenDialog(textArea.getScene().getWindow());

        if (file != null) {
            try {
                textArea.setText(Files.readString(file.toPath()));
                currentFile = file;
                isModified = false;
                updateTitle();
                textArea2.appendText("Файл открыт: " + file.getName() + "\n");
            } catch (IOException e) {
                showInfo("Ошибка открытия файла");
            }
        }
    }

    @FXML
    public void handleSave() {
        try {
            if (currentFile == null) {
                handleSaveAs();
                return;
            }

            Files.writeString(currentFile.toPath(), textArea.getText());
            textArea2.setText("Файл сохранён");
        } catch (Exception e) {
            textArea2.setText("Ошибка сохранения");
        }
    }

    @FXML
    private void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить файл");

        File file = fileChooser.showSaveDialog(textArea.getScene().getWindow());

        if (file != null) {
            saveToFile(file);
        }
    }

    private void saveToFile(File file) {
        try {
            Files.writeString(file.toPath(), textArea.getText());
            currentFile = file;
            isModified = false;
            updateTitle();
            textArea2.appendText("Файл сохранён: " + file.getName() + "\n");
        } catch (IOException e) {
            showInfo("Ошибка сохранения файла");
        }
    }

    @FXML
    public void handleExit() {
        if (!checkSaveBeforeAction()) return;

        Stage stage = (Stage) textArea.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void handleUndo() {
        textArea.undo();
    }

    @FXML
    public void handleRedo() {
        textArea.redo();
    }

    @FXML
    public void handleCut() {
        textArea.cut();
    }

    @FXML
    public void handleCopy() {
        textArea.copy();
    }

    @FXML
    public void handlePaste() {
        textArea.paste();
    }

    @FXML
    public void handleDelete() {
        textArea.replaceSelection("");
    }

    @FXML
    public void handleSelectAll() {
        textArea.selectAll();
    }

    @FXML
    public void handleProblemStatement() {
        showInfo("Постановка задачи");
    }

    @FXML
    public void handleGrammar() {
        showInfo("Грамматика");
    }

    @FXML
    public void handleGrammarClassification() {
        showInfo("Классификация грамматики");
    }

    @FXML
    public void handleAnalysisMethod() {
        showInfo("Метод анализа");
    }

    @FXML
    public void handleTestExample() {
        showInfo("Тестовый пример");
    }

    @FXML
    public void handleLiteratureList() {
        showInfo("Список литературы");
    }

    @FXML
    public void handleListing() {
        showInfo("Исходный код программы");
    }

    @FXML
    public void handleRun() { 
        String text = textArea.getText();
        if (text == null || text.isEmpty()) {
            textArea2.setText("Текст для анализа отсутствует.");
            tokenData.clear();
            syntaxErrorData.clear();
            return;
        }

        List<Token> tokens = Scanner.analyze(text);
        tokenData.clear();
        tokenData.addAll(tokens);

        long errorCount = tokens.stream().filter(t -> t.getCode() == 17).count();

        List<SyntaxError> syntaxErrors = Parser.parse(tokens);
        syntaxErrorData.clear();
        syntaxErrorData.addAll(syntaxErrors);

        int syntaxErrorCount = syntaxErrors.size();

        if (errorCount > 0 || syntaxErrorCount > 0) {
            textArea2.setText("Анализ завершен. Лексических ошибок: " + errorCount + ", синтаксических ошибок: " + syntaxErrorCount + ".");
            showInfo("Анализ завершен с ошибками. Проверьте сообщения и таблицы.");
        } else {
            textArea2.setText("Анализ успешно завершен. Ошибок не обнаружено.");
        }
    }

    @FXML
    private void handleCallingHelp() {

        Stage helpStage = new Stage();
        helpStage.setTitle("Справка");

        TextArea helpText = new TextArea();
        helpText.setEditable(false);
        helpText.setWrapText(true);

        helpText.setText("""
            СПРАВОЧНАЯ СИСТЕМА
            
            Создать – создаёт новый документ.
            Открыть – открывает существующий файл.
            Сохранить – сохраняет текущий файл.
            Сохранить как – сохраняет файл под новым именем.
            Выход – закрывает программу с предложением сохранить изменения.
            
            Отменить – отменяет последнее действие.
            Повторить – повторяет отменённое действие.
            Вырезать – удаляет выделенный текст и помещает в буфер.
            Копировать – копирует выделенный текст.
            Вставить – вставляет текст из буфера.
            Удалить – удаляет выделенный фрагмент.
            Выделить все – выделяет весь текст.
            
            Содержит быстрый доступ к основным командам:
            создать, открыть, сохранить, undo/redo, копировать, вырезать, вставить.
            
            Верхняя область – редактирование текста.
            Нижняя область – вывод сообщений программы.
            
            Программа автоматически предлагает сохранить изменения
            при выходе или открытии нового файла.
            """);

        Scene scene = new Scene(new StackPane(helpText), 500, 400);
        helpStage.setScene(scene);
        helpStage.show();
    }

    @FXML
    public void handleAbout() {
        showInfo("Text Editor\nВерсия 1.0");
    }

    private void showInfo(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}