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
import javafx.scene.paint.Paint;
import javafx.util.Callback;
import org.kordamp.ikonli.javafx.FontIcon;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.system.LiveMessageService;
import top.pigest.queuemanagerdemo.system.NarratorService;
import top.pigest.queuemanagerdemo.system.settings.DanmakuServiceSettings;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.widget.QMButton;
import top.pigest.queuemanagerdemo.widget.WhiteFontIcon;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class DanmakuServiceContainer extends MultiMenuProvider<Pane> implements NamedContainer {
    private VBox connectService;
    private Pane narratorService;

    public DanmakuServiceContainer() {
        super();
        this.setInnerContainer(this.getMenus().entrySet().iterator().next().getValue().get());
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
        BorderPane.setMargin(vBox, new Insets(10, 40, 0, 40));
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
                QueueManager.INSTANCE.getMainScene().showDialogMessage(e.getMessage(), true);
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
                        QueueManager.INSTANCE.getMainScene().showDialogMessage("连接失败\n" + e.getMessage(), true);
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
        BorderPane main = new BorderPane();
        main.setId("c1");

        VBox page0 = new VBox();
        page0.setAlignment(Pos.TOP_LEFT);
        BorderPane enabled = MultiMenuProvider.createLRBorderPane("启用播报", -1, -1, Utils.make(new JFXToggleButton(), button -> {
            button.setSize(8);
            button.setSelected(getDanmakuServiceSettings().narratorEnabled);
            button.setDisableVisualFocus(true);
            button.setOnAction(event -> getDanmakuServiceSettings().setNarratorEnabled(button.isSelected()));
        }));
        BorderPane rate = MultiMenuProvider.createLRBorderPane("播报语速", -1, -1, Utils.make(new JFXSlider(-10, 10, getDanmakuServiceSettings().narratorRate), slider -> {
            slider.setPrefWidth(500);
            slider.setBlockIncrement(1.0);
            slider.setStyle("-fx-font-family: 'Microsoft YaHei UI'");
            slider.setIndicatorPosition(JFXSlider.IndicatorPosition.RIGHT);
            slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                long round = Math.round(newValue.doubleValue());
                slider.setValue(round);
                getDanmakuServiceSettings().setNarratorRate(Math.toIntExact(round));
            });
        }));
        BorderPane voice = MultiMenuProvider.createLRBorderPane("播报语音", -1, -1, createVoiceSelector());
        BorderPane volume = MultiMenuProvider.createLRBorderPane("播报音量", -1, -1, Utils.make(new JFXSlider(0, 100, getDanmakuServiceSettings().narratorVolume), slider -> {
            slider.setPrefWidth(500);
            slider.setBlockIncrement(1.0);
            slider.setStyle("-fx-font-family: 'Microsoft YaHei UI'");
            slider.setIndicatorPosition(JFXSlider.IndicatorPosition.RIGHT);
            slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                long round = Math.round(newValue.doubleValue());
                slider.setValue(round);
                getDanmakuServiceSettings().setNarratorVolume(Math.toIntExact(round));
            });
        }));
        BorderPane mode = MultiMenuProvider.createLRBorderPane("播报模式", -1, -1, createModeSelector());
        BorderPane test = MultiMenuProvider.createLRBorderPane("测试播报", -1, -1, Utils.make(new VBox(10), vBox -> {
            vBox.setAlignment(Pos.CENTER_RIGHT);
            JFXTextField text = new JFXTextField();
            text.setFont(Settings.DEFAULT_FONT);
            text.setText(Objects.requireNonNullElse(QueueManager.INSTANCE.getMainScene().getUserName(), "小猪之最Thepig") + "投喂了1个牛哇牛哇");
            text.setPromptText("测试文本");
            text.setFocusColor(Paint.valueOf("#1a8bcc"));
            text.setPrefWidth(500);
            HBox buttons = new HBox(20);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            QMButton start = new QMButton("播放测试语音", "#55bb55", false);
            start.setPrefWidth(240);
            start.setGraphic(new WhiteFontIcon("fas-volume-up"));
            start.setOnAction(event -> NarratorService.addString(text.getText()));
            QMButton stop = new QMButton("停止所有播放", "#bb5555", false);
            stop.setPrefWidth(240);
            stop.setGraphic(new WhiteFontIcon("fas-volume-mute"));
            stop.setOnAction(event -> NarratorService.stopSpeaking());
            buttons.getChildren().addAll(start, stop);
            vBox.getChildren().addAll(text, buttons);
        }));
        page0.getChildren().addAll(enabled, rate, voice, volume, mode, test);

        VBox page1 = new VBox();
        VBox vb1 = new VBox(10);
        vb1.setAlignment(Pos.TOP_RIGHT);
        for (DanmakuServiceSettings.NarratableElement element : DanmakuServiceSettings.NarratableElement.values()) {
            vb1.getChildren().add(createAcceptedTypeSettings(element));
        }
        BorderPane acceptedTypes = MultiMenuProvider.createLRBorderPane("播报信息类型", -1, -1, vb1);
        BorderPane giftComboOptimization = MultiMenuProvider.createLRBorderPane("礼物连击优化", -1, -1, Utils.make(new HBox(), hBox -> {
            hBox.setAlignment(Pos.CENTER_RIGHT);
            hBox.setSpacing(20);
            JFXCheckBox checkBox = new JFXCheckBox();
            checkBox.setFont(Settings.DEFAULT_FONT);
            checkBox.setPrefWidth(70);
            checkBox.setText("启用");
            checkBox.setSelected(getDanmakuServiceSettings().giftComboOptimization);
            checkBox.setOnAction(event -> {
                CheckBox box = (CheckBox) event.getSource();
                getDanmakuServiceSettings().setGiftComboOptimization(box.isSelected());
            });
            JFXTextField textField = new JFXTextField();
            textField.setPromptText("播报内容");
            textField.setFocusColor(Paint.valueOf("#1a8bcc"));
            textField.setFont(Settings.SPEC_FONT);
            textField.setText(getDanmakuServiceSettings().giftComboEndText);
            textField.setPrefWidth(450);
            textField.textProperty().addListener((observable, oldValue, newValue) -> getDanmakuServiceSettings().setGiftComboEndText(newValue));
            hBox.getChildren().add(checkBox);
            hBox.getChildren().add(textField);
        }));
        BorderPane multiGuardOptimization = MultiMenuProvider.createLRBorderPane("批量上舰优化", -1, -1, Utils.make(new HBox(), hBox -> {
            hBox.setAlignment(Pos.CENTER_RIGHT);
            hBox.setSpacing(20);
            JFXCheckBox checkBox = new JFXCheckBox();
            checkBox.setFont(Settings.DEFAULT_FONT);
            checkBox.setPrefWidth(70);
            checkBox.setText("启用");
            checkBox.setSelected(getDanmakuServiceSettings().multiGuardOptimization);
            checkBox.setOnAction(event -> {
                CheckBox box = (CheckBox) event.getSource();
                getDanmakuServiceSettings().setMultiGuardOptimization(box.isSelected());
            });
            JFXTextField textField = new JFXTextField();
            textField.setPromptText("播报内容");
            textField.setFocusColor(Paint.valueOf("#1a8bcc"));
            textField.setFont(Settings.SPEC_FONT);
            textField.setText(getDanmakuServiceSettings().multiGuardText);
            textField.setPrefWidth(450);
            textField.textProperty().addListener((observable, oldValue, newValue) -> getDanmakuServiceSettings().setMultiGuardText(newValue));
            hBox.getChildren().add(checkBox);
            hBox.getChildren().add(textField);
        }));
        BorderPane resetSettings = MultiMenuProvider.createLRBorderPane("恢复默认设置", -1, -1, Utils.make(new QMButton("恢复所有弹幕播报设置", "#bb5555", true), button -> {
            button.setGraphic(new WhiteFontIcon("fas-undo"));
            button.setOnAction(event -> {
                Settings.resetDanmakuServiceSettings();
                this.narratorService = this.initC1();
                this.setInnerContainer(this.narratorService);
                QueueManager.INSTANCE.getMainScene().showDialogMessage("已恢复设置", false);
            });
        }));
        page1.getChildren().addAll(acceptedTypes, giftComboOptimization, multiGuardOptimization, resetSettings);


        QMButton left = new QMButton("", null, false);
        QMButton right = new QMButton("", null, false);
        left.setGraphic(Utils.make(new FontIcon("fas-angle-left"), fontIcon -> fontIcon.setIconSize(20)));
        right.setGraphic(Utils.make(new FontIcon("fas-angle-right"), fontIcon -> fontIcon.setIconSize(20)));
        left.setMaxWidth(30);
        right.setMaxWidth(30);
        left.setAlignment(Pos.CENTER);
        right.setAlignment(Pos.CENTER);
        left.disable(true);
        left.setOnAction(event -> {
            main.setCenter(page0);
            left.disable(true);
            right.disable(false);
        });
        right.disable(false);
        right.setOnAction(event -> {
            main.setCenter(page1);
            left.disable(false);
            right.disable(true);
        });
        main.setLeft(left);
        main.setRight(right);
        main.setCenter(page0);
        BorderPane.setMargin(main, new Insets(10, 0, 0, 0));
        return main;
    }

    private static JFXComboBox<DanmakuServiceSettings.NarratorType> createModeSelector() {
        return Utils.make(new JFXComboBox<>(), comboBox -> {
            comboBox.setPrefWidth(500);
            comboBox.setValue(Settings.getDanmakuServiceSettings().narratorType);
            comboBox.setCellFactory(new Callback<>() {
                @Override
                public ListCell<DanmakuServiceSettings.NarratorType> call(ListView<DanmakuServiceSettings.NarratorType> param) {
                    return new ListCell<>() {
                        @Override
                        protected void updateItem(DanmakuServiceSettings.NarratorType item, boolean empty) {
                            super.updateItem(item, empty);
                            this.setFont(Settings.DEFAULT_FONT);
                            if (item != null && !empty) {
                                this.setText(item.toString());
                            }
                        }
                    };
                }
            });
            comboBox.getItems().addAll(DanmakuServiceSettings.NarratorType.values());
            comboBox.getButtonCell().setFont(Settings.DEFAULT_FONT);
            comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                Settings.getDanmakuServiceSettings().setNarratorType(newValue);
                NarratorService.stopSpeaking();
            });
        });
    }

    private static JFXComboBox<NarratorService.Voice> createVoiceSelector() {
        return Utils.make(new JFXComboBox<>(), comboBox -> {
            CompletableFuture<List<NarratorService.Voice>> future = CompletableFuture.supplyAsync(NarratorService::getAvailableVoices);
            future.thenAccept(voices -> {
                CompletableFuture<List<NarratorService.Voice>> future1 = CompletableFuture.supplyAsync(NarratorService::getRegistrableVoices);
                future1.thenAccept(voices1 -> {
                    voices.addAll(voices1.stream().filter(v -> voices.stream().noneMatch(v1 -> v1.name().equals(v.name()))).toList());
                    Platform.runLater(() -> {
                        comboBox.getItems().addAll(voices);
                        Optional<NarratorService.Voice> optional = voices.stream().filter(voice -> !voice.requireRegistration() && voice.name().equals(getDanmakuServiceSettings().narratorVoiceName)).findFirst();
                        optional.ifPresent(comboBox::setValue);
                    });
                });
            });
            comboBox.setPrefWidth(500);
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
                                    CompletableFuture<List<NarratorService.Voice>> refresh = CompletableFuture.supplyAsync(NarratorService::getAvailableVoices);
                                    refresh.thenAccept(voices -> {
                                        CompletableFuture<List<NarratorService.Voice>> future1 = CompletableFuture.supplyAsync(NarratorService::getRegistrableVoices);
                                        future1.thenAccept(voices1 -> Platform.runLater(() -> {
                                            comboBox.getItems().clear();
                                            voices.addAll(voices1.stream().filter(v -> voices.stream().noneMatch(v1 -> v1.name().equals(v.name()))).toList());
                                            comboBox.getItems().addAll(voices);
                                            Optional<NarratorService.Voice> optional = voices.stream().filter(voice -> !voice.requireRegistration() && voice.name().equals(newValue.name())).findFirst();
                                            optional.ifPresent(v -> {
                                                comboBox.setValue(v);
                                                Platform.runLater(() -> Utils.showDialogMessage("导入成功，已自动设置", false, QueueManager.INSTANCE.getMainScene().getRootDrawer()));
                                            });
                                        }));
                                    });
                                }
                            } catch (Exception e) {
                                Platform.runLater(() -> Utils.showDialogMessage("导入失败\n" + e.getMessage(), true, QueueManager.INSTANCE.getMainScene().getRootDrawer()));
                            }
                        }).start();
                    }, event -> {
                    }, QueueManager.INSTANCE.getMainScene().getRootDrawer());
                } else {
                    Settings.getDanmakuServiceSettings().setNarratorVoiceName(newValue.name());
                }
            });
        });
    }

    private Pane createAcceptedTypeSettings(DanmakuServiceSettings.NarratableElement element) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.setSpacing(20);
        JFXCheckBox checkBox = new JFXCheckBox();
        checkBox.setFont(Settings.DEFAULT_FONT);
        checkBox.setPrefWidth(70);
        checkBox.setText(element.getMessage());
        checkBox.setSelected(getDanmakuServiceSettings().acceptedTypes.contains(element));
        checkBox.setOnAction(event -> {
            CheckBox box = (CheckBox) event.getSource();
            getDanmakuServiceSettings().modifyAcceptedType(element, box.isSelected());
        });
        JFXTextField textField = new JFXTextField();
        textField.setPromptText("播报内容");
        textField.setFocusColor(Paint.valueOf("#1a8bcc"));
        textField.setFont(Settings.SPEC_FONT);
        textField.setText(getDanmakuServiceSettings().getNarratorText(element));
        textField.setPrefWidth(450);
        textField.textProperty().addListener((observable, oldValue, newValue) -> getDanmakuServiceSettings().setNarratorText(element, newValue));
        hBox.getChildren().add(checkBox);
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
