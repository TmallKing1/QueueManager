package top.pigest.queuemanagerdemo.control;

import com.jfoenix.controls.JFXTextField;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import top.pigest.queuemanagerdemo.Settings;

import java.util.function.Consumer;

public class IntegerModifier extends HBox {
    private int value;
    private Consumer<Integer> onValueSet = value -> {};
    private final int step;
    private final int min;
    private final int max;
    private final QMButton decrementButton = new QMButton("", null, false);
    private final JFXTextField valueField = new JFXTextField();
    private final QMButton incrementButton = new QMButton("", null, false);
    public IntegerModifier(int value) {
        this(value, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    public IntegerModifier(int value, int min, int max) {
        this(value, 1, min, max);
    }
    public IntegerModifier(int value, int step, int min, int max) {
        this.value = value;
        this.step = step;
        this.min = min;
        this.max = max;
        this.decrementButton.setGraphic(new FontIcon("fas-minus"));
        this.decrementButton.setOnAction(event -> this.decrement());
        this.incrementButton.setGraphic(new FontIcon("fas-plus"));
        this.incrementButton.setOnAction(event -> this.increment());
        this.valueField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isValid(newValue)) {
                setValue(Integer.parseInt(newValue), false);
            }
        });
        this.valueField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && !isValid(this.valueField.getText())) {
                setValue(getValue(), true);
            }
        });
        this.valueField.setText(value+"");
        this.valueField.setPrefWidth(100);
        this.valueField.setFont(Settings.DEFAULT_FONT);
        this.valueField.setAlignment(Pos.CENTER);

        this.getChildren().addAll(decrementButton, valueField, incrementButton);
    }

    public void setOnValueSet(Consumer<Integer> onValueSet) {
        this.onValueSet = onValueSet;
    }

    public void setValue(int value, boolean modifyField) {
        this.value = value;
        onValueSet.accept(value);
        incrementButton.disable(this.value >= max);
        decrementButton.disable(this.value <= min);
        if (modifyField) {
            this.valueField.setText(value+"");
        }
    }

    public int getValue() {
        return value;
    }

    public void decrement() {
        this.setValue(Math.max(value - step, min), true);
    }

    public void increment() {
        this.setValue(Math.min(value + step, max), true);
    }

    public boolean isValid(String s) {
        try {
            int i = Integer.parseInt(s);
            return i >= min && i <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
