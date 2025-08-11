package top.pigest.queuemanagerdemo.window.main;

import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Callback;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.system.LiveMessageService;
import top.pigest.queuemanagerdemo.system.NarratorService;
import top.pigest.queuemanagerdemo.system.settings.DanmakuServiceSettings;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.widget.QMButton;
import top.pigest.queuemanagerdemo.widget.WhiteFontIcon;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class DanmakuServiceContainer extends MultiMenuProvider<Pane> implements NamedContainer {
    private VBox connectService;
    private Pane narratorService;

    public DanmakuServiceContainer() {
        super();
    }

    public VBox initC0() {
        VBox vBox = new VBox();
        vBox.setId("c0");
        vBox.setAlignment(Pos.TOP_LEFT);
        vBox.getChildren().add(MultiMenuProvider.createLRBorderPane("连接直播间", -1, -1, connectButton()));
        vBox.getChildren().add(MultiMenuProvider.createLRBorderPane("自动连接", -1, -1, Utils.make(new JFXToggleButton(), button -> {
            button.setSelected(getDanmakuServiceSettings().autoConnect);
            button.setDisableVisualFocus(true);
            button.setOnAction(event -> getDanmakuServiceSettings().setAutoConnect(button.isSelected()));
        })));
        vBox.setBorder(new Border(MultiMenuProvider.DOWN_BORDER_STROKE));
        BorderPane.setMargin(vBox, new Insets(20, 40, 0, 40));
        return vBox;
    }

    private QMButton connectButton() {
        boolean connected = isConnected();
        QMButton qmButton = new QMButton("开始连接", "#55bb55", false);
        qmButton.setPrefWidth(160);
        qmButton.setGraphic(new WhiteFontIcon("fas-link"));
        if (connected) {
            connectedButton(qmButton);
        } else {
            setConnectAction(qmButton);
        }
        return qmButton;
    }

    private static boolean isConnected() {
        return LiveMessageService.getInstance() != null && LiveMessageService.getInstance().isSessionAvailable();
    }

    public void connectedButton(QMButton qmButton) {
        qmButton.setText("断开连接");
        qmButton.setGraphic(new WhiteFontIcon("fas-unlink"));
        qmButton.setBackgroundColor("#bb5555");
        qmButton.setOnAction(event1 -> {
            try {
                LiveMessageService.getInstance().close();
            } catch (IOException e) {
                QueueManager.INSTANCE.getMainScene().showDialogMessage(e.getMessage(), "RED");
            }
            disconnectedButton(qmButton);
        });
        qmButton.disable(false);
    }

    public void disconnectedButton(QMButton qmButton) {
        qmButton.setText("开始连接");
        qmButton.setGraphic(new WhiteFontIcon("fas-link"));
        qmButton.setBackgroundColor("#55bb55");
        setConnectAction(qmButton);
    }

    private void setConnectAction(QMButton qmButton) {
        qmButton.setOnAction(event -> {
            qmButton.disable(true);
            qmButton.setText("正在连接");
            qmButton.setGraphic(new WhiteFontIcon("fas-bullseye"));
            new Thread(() -> {
                try {
                    LiveMessageService.connect();
                } catch (LiveMessageService.ConnectFailedException e) {
                    Platform.runLater(() -> {
                        QueueManager.INSTANCE.getMainScene().showDialogMessage("连接失败\n" + e.getMessage(), "RED");
                        qmButton.disable(false);
                        qmButton.setText("开始连接");
                        qmButton.setGraphic(new WhiteFontIcon("fas-link"));
                    });
                }
            }).start();
        });
    }

    public Pane getC0() {
        if (connectService == null) {
            connectService = initC0();
        }
        return connectService;
    }

    private Pane initC1() {
        VBox vBox = new VBox();
        vBox.setId("c1");
        vBox.setAlignment(Pos.TOP_LEFT);
        BorderPane enabled = MultiMenuProvider.createLRBorderPane("启用播报", -1, -1, Utils.make(new JFXToggleButton(), button -> {
            button.setSize(8);
            button.setSelected(getDanmakuServiceSettings().narratorEnabled);
            button.setDisableVisualFocus(true);
            button.setOnAction(event -> getDanmakuServiceSettings().setNarratorEnabled(button.isSelected()));
        }));
        vBox.getChildren().add(enabled);
        BorderPane rate = MultiMenuProvider.createLRBorderPane("播报语速", -1, -1, Utils.make(new JFXSlider(-10, 10, getDanmakuServiceSettings().narratorRate), slider -> {
            slider.setPrefWidth(400);
            slider.setBlockIncrement(1.0);
            slider.getChildrenUnmodifiable().forEach(node -> {if (node instanceof Text t) {t.setFont(Settings.DEFAULT_FONT);}});
            slider.setIndicatorPosition(JFXSlider.IndicatorPosition.RIGHT);
            slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                long round = Math.round(newValue.doubleValue());
                slider.setValue(round);
                getDanmakuServiceSettings().setNarratorRate(Math.toIntExact(round));
            });
        }));
        vBox.getChildren().add(rate);
        BorderPane voice = MultiMenuProvider.createLRBorderPane("播报语音", -1, -1, getVoices());
        vBox.getChildren().add(voice);
        BorderPane volume = MultiMenuProvider.createLRBorderPane("播报音量", -1, -1, Utils.make(new JFXSlider(0, 100, getDanmakuServiceSettings().narratorVolume), slider -> {
            slider.setPrefWidth(400);
            slider.setBlockIncrement(1.0);
            slider.setIndicatorPosition(JFXSlider.IndicatorPosition.RIGHT);
            slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                long round = Math.round(newValue.doubleValue());
                slider.setValue(round);
                getDanmakuServiceSettings().setNarratorVolume(Math.toIntExact(round));
            });
        }));
        vBox.getChildren().add(volume);
        VBox vb1 = new VBox(10);
        vb1.setAlignment(Pos.TOP_RIGHT);
        for (DanmakuServiceSettings.NarratableElement element : DanmakuServiceSettings.NarratableElement.values()) {
            vb1.getChildren().add(createAcceptedTypeSettings(element));
        }
        BorderPane acceptedTypes = MultiMenuProvider.createLRBorderPane("播报信息类型", -1, -1, vb1);
        vBox.getChildren().add(acceptedTypes);

        BorderPane.setMargin(vBox, new Insets(20, 40, 0, 40));
        return vBox;
    }

    private static JFXComboBox<NarratorService.Voice> getVoices() {
        JFXComboBox<NarratorService.Voice> box = Utils.make(new JFXComboBox<>(), comboBox -> {
            List<NarratorService.Voice> voices = NarratorService.getAvailableVoices();
            voices.addAll(NarratorService.getRegistrableVoices().stream().filter(voice -> voices.stream().noneMatch(voice1 -> voice1.name().equals(voice.name()))).toList());
            comboBox.getItems().addAll(voices);
            Optional<NarratorService.Voice> optional = voices.stream().filter(voice -> !voice.requireRegistration() && voice.name().equals(getDanmakuServiceSettings().narratorVoiceName)).findFirst();
            optional.ifPresent(comboBox::setValue);
            comboBox.setPrefWidth(400);
            comboBox.setCellFactory(new Callback<>() {
                @Override
                public ListCell<NarratorService.Voice> call(ListView<NarratorService.Voice> param) {
                    return new ListCell<>() {
                        @Override
                        protected void updateItem(NarratorService.Voice item, boolean empty) {
                            super.updateItem(item, empty);
                            this.setFont(Settings.DEFAULT_FONT);
                            if (item != null && !empty) {
                                if (item.requireRegistration()) {
                                    this.setText(item.name() + "*");
                                    this.setTextFill(Color.RED);
                                } else {
                                    this.setText(item.name());
                                    this.setTextFill(Color.BLACK);

                                }
                            }
                        }
                    };
                }
            });
            comboBox.getButtonCell().setFont(Settings.DEFAULT_FONT);
            comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) {
                    return;
                }
                if (newValue.requireRegistration()) {
                    comboBox.setValue(oldValue);
                    Utils.showChoosingDialog("该语音目前安装在你的电脑上，但无法使用\n将其添加到注册表后即可使用（需要管理员权限）\n按【确认】按钮可快速添加至注册表", "确认", "取消", event -> {
                        new Thread(() -> {
                            String command = "Start-Process cmd -Verb runAs -ArgumentList '/c reg import \"%s\"'".formatted(newValue.registryPath().substring(1));
                            try {
                                Process process = Runtime.getRuntime().exec(new String[]{"powershell", command});
                                int exitCode = process.waitFor();
                                if (exitCode != 0) {
                                    throw new RuntimeException("Exit code " + exitCode);
                                } else {
                                    Platform.runLater(() -> {
                                        comboBox.getItems().clear();
                                        List<NarratorService.Voice> voices1 = NarratorService.getAvailableVoices();
                                        voices1.addAll(NarratorService.getRegistrableVoices().stream().filter(v -> voices1.stream().noneMatch(voice1 -> voice1.name().equals(v.name()))).toList());
                                        comboBox.getItems().addAll(voices1);
                                        Optional<NarratorService.Voice> optional1 = voices1.stream().filter(voice -> !voice.requireRegistration() && voice.name().equals(newValue.name())).findFirst();
                                        optional1.ifPresent(voice -> {
                                            comboBox.setValue(voice);
                                            Utils.showDialogMessage("导入成功，已自动设置", "BLACK", QueueManager.INSTANCE.getMainScene().getRootStackPane());
                                        });
                                    });
                                }
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    Utils.showDialogMessage("导入失败\n" + e.getMessage(), "RED", QueueManager.INSTANCE.getMainScene().getRootStackPane());
                                });
                            }
                        }).start();
                    }, event -> {
                    }, QueueManager.INSTANCE.getMainScene().getRootStackPane());
                }
            });
        });
        return box;
    }

    private Pane createAcceptedTypeSettings(DanmakuServiceSettings.NarratableElement element) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.setSpacing(20);
        JFXCheckBox checkBox = new JFXCheckBox();
        checkBox.setFont(Settings.DEFAULT_FONT);
        checkBox.setPrefWidth(80);
        checkBox.setText(element.getMessage());
        checkBox.setSelected(getDanmakuServiceSettings().acceptedTypes.contains(element));
        checkBox.setOnAction(event -> {
            CheckBox box = (CheckBox) event.getSource();
            getDanmakuServiceSettings().modifyAcceptedType(element, box.isSelected());
        });
        hBox.getChildren().add(checkBox);
        JFXTextField textField = new JFXTextField();
        textField.setFont(Settings.SPEC_FONT);
        textField.setText(getDanmakuServiceSettings().getNarratorText(element));
        textField.setPrefWidth(450);
        textField.textProperty().addListener((observable, oldValue, newValue) -> getDanmakuServiceSettings().setNarratorText(element, newValue));
        hBox.getChildren().add(textField);
        return hBox;
    }

    private static DanmakuServiceSettings getDanmakuServiceSettings() {
        return Settings.getDanmakuServiceSettings();
    }

    private Pane getC1() {
        if (narratorService == null) {
            narratorService = initC1();
        }
        return narratorService;
    }

    @Override
    public LinkedHashMap<String, Supplier<Pane>> getMenus() {
        LinkedHashMap<String, Supplier<Pane>> menus = new LinkedHashMap<>();
        menus.put("连接服务", this::getC0);
        menus.put("弹幕播报", this::getC1);
        return menus;
    }

    @Override
    public int getMenuIndex(Pane innerContainer) {
        String id = innerContainer.getId();
        if (id == null) {
            return -1;
        }
        return id.charAt(1) - '0';
    }

    @Override
    public String getName() {
        return "弹幕服务";
    }
}
