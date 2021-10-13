package com.flowapp.NonNewtonian.Controllers;

import com.flowapp.NonNewtonian.Models.FlowResult;
import com.flowapp.NonNewtonian.Models.ProblemResult;
import com.flowapp.NonNewtonian.NonNewtonian;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.regex.Pattern;

public class MainWindowController implements Initializable {


    @FXML
    private TextField iDTextField;

    @FXML
    private TextField loopIDTextField;

    @FXML
    private TextField totalLengthTextField;

    @FXML
    private TextField flowRateTextField;

    @FXML
    private TextField flowRateIncreaseTextField;

    @FXML
    private TextField loopFlowRateTextField;

    @FXML
    private TextField spGrTextField;

    @FXML
    private TextField nDashTextField;

    @FXML
    private TextField maxPressureTextField;

    @FXML
    private TextField kDashTextField;

    @FXML
    private TextField anDashTextField;

    @FXML
    private TextField bnDashTextField;

    @FXML
    private TextField viscosityTextField;

    @FXML
    private TextField npshTextField;

    @FXML
    private TextField staticHeadTextField;

    @FXML
    private TextArea answerArea;

    @FXML
    private Button calculateBtn;

    @FXML
    private ImageView facebookIcon;

    @FXML
    private ImageView linkedInIcon;

    @FXML
    private ImageView emailIcon;

    private Map<String, Stage> charts = new HashMap<>();

    private final Application application;

    public MainWindowController(Application application) {
        this.application = application;
    }

    Stage getStage() {
        return (Stage) answerArea.getScene().getWindow();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        final TextField[] textFields = {
                iDTextField,loopIDTextField,totalLengthTextField,
                flowRateTextField,flowRateIncreaseTextField,loopFlowRateTextField,
                spGrTextField,nDashTextField,maxPressureTextField,kDashTextField,
                anDashTextField,bnDashTextField,viscosityTextField,npshTextField,
                staticHeadTextField,
        };
        for (var field: textFields) {
            field.setTextFormatter(createDecimalFormatter());
        }
        var packagePath = getClass().getPackageName().split("\\.");
        packagePath[packagePath.length-1] = "Fonts";
        String fontPath = Arrays.stream(packagePath).reduce("", (s, s2) -> s + "/" + s2);
        Font font = Font.loadFont(getClass().getResourceAsStream(fontPath + "/FiraCode-Retina.ttf"), answerArea.getFont().getSize());
        answerArea.setFont(font);
        calculateBtn.setOnAction(e -> {
            try {
                calculate();
            } catch (Exception ex) {
                ex.printStackTrace();
                final var errorDialog = createErrorDialog(getStage(), ex);
                errorDialog.show();
            }
        });
        setUpIcons();
    }

    private void setUpIcons() {
        var packagePath = getClass().getPackageName().split("\\.");
        packagePath[packagePath.length-1] = "Images";
        String fontPath = Arrays.stream(packagePath).reduce("", (s, s2) -> s + "/" + s2);
        final var facebookImage = getClass().getResource(fontPath + "/facebook.png");
        final var linkedInImage = getClass().getResource(fontPath + "/linkedin.png");
        final var emailImage = getClass().getResource(fontPath + "/email.png");
        facebookIcon.setImage(new Image(Objects.requireNonNull(facebookImage).toString()));
        linkedInIcon.setImage(new Image(Objects.requireNonNull(linkedInImage).toString()));
        emailIcon.setImage(new Image(Objects.requireNonNull(emailImage).toString()));
        facebookIcon.setPickOnBounds(true);
        linkedInIcon.setPickOnBounds(true);
        emailIcon.setPickOnBounds(true);
        facebookIcon.setOnMouseClicked(e -> {
            openBrowser("https://www.facebook.com/Moustafa.essam.hpp");
        });
        linkedInIcon.setOnMouseClicked(e -> {
            openBrowser("https://www.linkedin.com/in/moustafa-essam-726262174");
        });
        emailIcon.setOnMouseClicked(e -> {
            final var email = "mailto:essam.moustafa15@gmail.com";
            openBrowser(email);
            copyToClipboard(email);
        });
    }

    void openBrowser(String url) {
        application.getHostServices().showDocument(url);
    }

    private void copyToClipboard(String answer) {
        Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, answer));
    }

    private final Pattern numbersExpr = Pattern.compile("[-]?[\\d]*[.]?[\\d]*");
    TextFormatter<?> createDecimalFormatter() {
        final var pattern = numbersExpr.pattern();
        return new TextFormatter<>(c -> {
            if (c.getControlNewText().isEmpty()) { return c; }
            final var isGood = c.getControlNewText().matches(pattern);
            if (isGood) { return c; }
            else { return null; }
        });
    }

    void calculate() {

        final float spGr = getFloat(spGrTextField.getText());
        final float kDash = getFloat(kDashTextField.getText());
        final float nDash = getFloat(nDashTextField.getText());
        final float anDash = getFloat(anDashTextField.getText());
        final float bnDash = getFloat(bnDashTextField.getText());
        final float viscosityCp = getFloat(viscosityTextField.getText());
        final float maxPressureBar = getFloat(maxPressureTextField.getText());
        final float iDmm = getFloat(iDTextField.getText());
        final float loopIDmm = getFloat(loopIDTextField.getText());
        final float lengthM = getFloat(totalLengthTextField.getText());
        final float flowRateM3H = getFloat(flowRateTextField.getText());
        final float increasedFlowRateM3H = (100 + getFloat(flowRateIncreaseTextField.getText())) / 100.0f * flowRateM3H;
        final float staticHead = getFloat(staticHeadTextField.getText());
        final float nPSH = getFloat(npshTextField.getText());
        final Float loopFlowRate = getFloat(loopFlowRateTextField.getText());

        final var task = new Service<ProblemResult>() {
            @Override
            protected Task<ProblemResult> createTask() {
                return new Task<>() {
                    @Override
                    protected ProblemResult call() {
                        return new NonNewtonian().nonNewtonian(spGr, kDash, nDash, anDash, bnDash, viscosityCp, maxPressureBar, iDmm, loopIDmm, lengthM, flowRateM3H, increasedFlowRateM3H, staticHead, nPSH, loopFlowRate);

                    }
                };
            }
        };
        final var loadingDialog = createProgressAlert(getStage(), task);
        task.setOnRunning(e -> {
            loadingDialog.show();
        });
        task.setOnSucceeded(e -> {
            final var result = task.getValue();
            drawLines(result.getNonNewtonianDirect(), result.getNonNewtonianReverse(), "NonNewtonian");
            drawLines(result.getNewtonianDirect(), result.getNewtonianReverse(), "Newtonian");
            setAnswer(result.getSteps());
            loadingDialog.close();
        });
        task.setOnFailed(e -> {
            final var error = e.getSource().getException();
            final var errorDialog = createErrorDialog(getStage(), error);
            errorDialog.show();
            setAnswer(error.getMessage());
            loadingDialog.close();
        });
        task.setOnCancelled(e -> {
            loadingDialog.close();
        });
        task.restart();
    }

    Float getFloat(String value) {
        try {
            return Float.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    Integer getInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    void setAnswer(String answer) {
        answerArea.setText(answer);
    }

    Alert createErrorDialog(Stage owner, Throwable e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle("Error");
        alert.setContentText(e.getMessage());
        return alert;
    }

    Alert createProgressAlert(Stage owner, Service<?> task) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.initOwner(owner);
        alert.titleProperty().bind(task.titleProperty());
        alert.contentTextProperty().bind(task.messageProperty());

        ProgressIndicator pIndicator = new ProgressIndicator();
        pIndicator.progressProperty().bind(task.progressProperty());
        alert.setGraphic(pIndicator);
        alert.setHeaderText("Loading...");

        alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
        alert.getDialogPane().lookupButton(ButtonType.OK)
                .disableProperty().bind(task.runningProperty());

        alert.getDialogPane().cursorProperty().bind(
                Bindings.when(task.runningProperty())
                        .then(Cursor.WAIT)
                        .otherwise(Cursor.DEFAULT)
        );
        return alert;
    }


    private void drawLines(FlowResult direct, FlowResult reverse, String title) {
        XYChart.Series<Number, Number> beforeSeries = new XYChart.Series();

        beforeSeries.setName("Before");
        for (var l: direct.getBefore()) {
            beforeSeries.getData().add(new XYChart.Data(l.getStartLength(), l.getStartHt() + l.getStartStatic()));
            beforeSeries.getData().add(new XYChart.Data(l.getStartLength() + l.getLength(), l.getNPSH() + l.getEndStatic()));
        }

        final List<XYChart.Series<Number, Number>> loopsSeries = new ArrayList<>();
        for (var l: direct.getLoops()) {
            XYChart.Series<Number, Number> loopSeries = new XYChart.Series();
            loopSeries.setName("Loop");
            loopSeries.getData().add(new XYChart.Data(l.getStartLength(), l.getStartHt() + l.getStartStatic()));
            loopSeries.getData().add(new XYChart.Data(l.getStartLength() + l.getLength(), l.getNPSH() + l.getEndStatic()));
            loopsSeries.add(loopSeries);
        }

        final List<XYChart.Series<Number, Number>> afterSeries = new ArrayList<>();
        for (int i =0; i < direct.getAfter().size(); i +=2) {
            XYChart.Series<Number, Number> lineSeries = new XYChart.Series();
            lineSeries.setName("After");
            final var line1 = direct.getAfter().get(i);
            lineSeries.getData().add(new XYChart.Data(line1.getStartLength(), line1.getNPSH() + line1.getStartStatic()));
            lineSeries.getData().add(new XYChart.Data(line1.getStartLength(), line1.getStartHt() + line1.getStartStatic()));
            lineSeries.getData().add(new XYChart.Data(line1.getStartLength() + line1.getLength(), line1.getNPSH() + line1.getEndStatic()));
            if (i < direct.getAfter().size()-1) {
                final var line2 = direct.getAfter().get(i+1);
                lineSeries.getData().add(new XYChart.Data(line2.getStartLength(), line2.getStartHt() + line2.getStartStatic()));
                lineSeries.getData().add(new XYChart.Data(line2.getStartLength() + line2.getLength(), line2.getNPSH() + line2.getEndStatic()));
            }
            loopsSeries.add(lineSeries);
        }

        XYChart.Series<Number, Number> reverseSeries = new XYChart.Series();
        reverseSeries.setName("Reverse");
        for (var l: reverse.getBefore()) {
            reverseSeries.getData().add(new XYChart.Data(l.getStartLength(), l.getNPSH() + l.getEndStatic()));
            reverseSeries.getData().add(new XYChart.Data(l.getStartLength() + l.getLength(), l.getStartHt() + l.getStartStatic()));
        }

        //Defining the x an y axes
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();

        //Setting labels for the axes
        xAxis.setLabel("L(m)");
        yAxis.setLabel("ht(m)");

        LineChart<Number, Number> hydraulicGradient = new LineChart<Number, Number>(xAxis, yAxis);
        hydraulicGradient.getData().addAll(beforeSeries, reverseSeries);
        hydraulicGradient.getData().addAll(afterSeries);
        hydraulicGradient.getData().addAll(loopsSeries);

        for (var item: hydraulicGradient.getData()) {
            for (XYChart.Data<Number, Number> entry : item.getData()) {
                Tooltip t = new Tooltip("(" + String.format("%.2f", Math.abs((float) entry.getXValue())) + " , " + entry.getYValue().toString() + ")");
                t.setShowDelay(new Duration(50));
                Tooltip.install(entry.getNode(), t);
            }
        }

        //Creating a stack pane to hold the chart
        StackPane pane = new StackPane(hydraulicGradient);
        pane.setPadding(new Insets(15, 15, 15, 15));
        pane.setStyle("-fx-background-color: BEIGE");
        //Setting the Scene
        Scene scene = new Scene(pane, 595, 350);
        Stage chartsWindow = charts.get(title);
        if (chartsWindow == null) {
            chartsWindow = new Stage();
            chartsWindow.initOwner(getStage());
            charts.put(title, chartsWindow);
        }
        chartsWindow.setTitle(title);
        chartsWindow.setScene(scene);
        chartsWindow.show();
    }
}
