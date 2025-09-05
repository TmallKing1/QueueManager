package top.pigest.queuemanagerdemo.window.music;

import com.google.gson.JsonObject;
import com.jfoenix.controls.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.Pair;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.control.IntegerModifier;
import top.pigest.queuemanagerdemo.music.Music163Api;
import top.pigest.queuemanagerdemo.music.Music163Login;
import top.pigest.queuemanagerdemo.music.MusicHandler;
import top.pigest.queuemanagerdemo.music.Song;
import top.pigest.queuemanagerdemo.settings.MusicServiceSettings;
import top.pigest.queuemanagerdemo.util.PagedContainerFactory;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.control.QMButton;
import top.pigest.queuemanagerdemo.control.WhiteFontIcon;
import top.pigest.queuemanagerdemo.window.main.MultiMenuProvider;
import top.pigest.queuemanagerdemo.window.main.NamedPage;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class MusicSystemPage extends MultiMenuProvider<Pane> implements NamedPage {
    private boolean preloaded = false;
    private boolean loggedIn = false;
    private Timeline preloadTimeline;
    private final QMButton loginButton = Utils.make(new QMButton("点击登录网易云账号", "#fc3c55"), button -> {
        button.setPrefSize(350, 40);
        button.setOnAction(actionEvent -> login());
        button.setDefaultButton(true);
        button.setGraphic(new WhiteFontIcon("fas-sign-in-alt"));
    });
    private Stage login;

    public Pane playerSettings;
    public Pane requestSettings;
    public Pane accountSettings;
    private Pane history;

    private void login() {
        openMusic163Login();
        switchLoginButtonState();
    }

    public void openMusic163Login() {
        this.login = new Stage();
        this.login.setResizable(false);
        this.login.setTitle("网易云账号登录");
        this.login.initOwner(QueueManager.INSTANCE.getMainScene().getWindow());
        this.login.initModality(Modality.WINDOW_MODAL);
        this.login.setScene(new Music163Login(this));
        this.login.setOnCloseRequest(event -> {
            switchLoginButtonState();
            ((Music163Login) this.login.getScene()).stopTimeline();
        });
        this.login.show();
    }

    public void closeLogin() {
        this.login.close();
    }

    public MusicSystemPage() {
        if (Settings.hasCookie("sDeviceId")) {
            this.preloaded = true;
            this.loggedIn = Settings.hasCookie("MUSIC_U");
        }
        this.setInnerContainer(this.getMenus().entrySet().iterator().next().getValue().get());
    }

    public Pane preload() {
        StackPane pane = new StackPane();
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        WebView webView = new WebView();
        webView.setMaxSize(0, 0);
        webView.setContextMenuEnabled(false);
        WebEngine webEngine = webView.getEngine();
        webEngine.setUserAgent(Settings.USER_AGENT);
        webEngine.load("https://music.163.com/#/login");
        this.preloadTimeline = new Timeline(new KeyFrame(new Duration(500), event -> {
            Optional<HttpCookie> sDeviceId = cookieManager.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equalsIgnoreCase("sDeviceId")).findFirst();
            if (sDeviceId.isPresent()) {
                CookieStore cookieStore = Settings.getCookieStore();
                HttpCookie httpCookie = sDeviceId.get();
                BasicClientCookie cookie = new BasicClientCookie(httpCookie.getName(), httpCookie.getValue());
                cookie.setPath(httpCookie.getPath());
                cookie.setDomain(httpCookie.getDomain());
                cookie.setExpiryDate(new Date(System.currentTimeMillis() + httpCookie.getMaxAge() * 1000));
                cookieStore.addCookie(cookie);
                Settings.saveCookie(false);
                this.preloaded = true;
                Platform.runLater(() -> QueueManager.INSTANCE.getMainScene().setMainContainer(new MusicSystemPage().withParentPage(this.getParentPage()), this.getId()));
                this.preloadTimeline.stop();
            }
        }));
        this.preloadTimeline.setCycleCount(Timeline.INDEFINITE);
        this.preloadTimeline.play();
        Label label = new Label("正在初始化\n请稍等几秒钟");
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.setFont(Settings.DEFAULT_FONT);
        pane.getChildren().addAll(webView, label);
        pane.setId("c0");
        return pane;
    }

    public Pane loginC0() {
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        Label label = Utils.createLabel("登录你的网易云音乐账号以开始使用\n仅能够扫码登录");
        label.setTextAlignment(TextAlignment.CENTER);
        vBox.setId("c0");
        vBox.getChildren().addAll(label, loginButton);
        return vBox;
    }

    private Pane initC0() {
        return new PagedContainerFactory("c0")
                .addControl("打开播放器", Utils.make(new QMButton("打开播放器", "#fc3c55", false), button -> {
                    button.setGraphic(new WhiteFontIcon("far-window-restore"));
                    button.setPrefWidth(180);
                    button.setOnAction(event -> {
                        MusicHandler.INSTANCE.showStage();
                        MusicHandler.INSTANCE.getPlayerStage().requestFocus();
                    });
                }))
                .addControl("自动播放", Utils.make(new JFXToggleButton(), button -> {
                    button.setSize(8);
                    button.setSelected(getMusicServiceSettings().autoPlay);
                    button.setDisableVisualFocus(true);
                    button.setOnAction(event -> getMusicServiceSettings().setAutoPlay(button.isSelected()));
                }))
                .addControl("播放器背景颜色", Utils.make(new JFXColorPicker(Color.valueOf(getMusicServiceSettings().backgroundColor)), ctrl -> {
                    ctrl.setStyle("-fx-font-family: '%s'; -fx-font-size: 20".formatted(Settings.DEFAULT_FONT.getFamily()));
                    ctrl.setPrefHeight(40);
                    ctrl.setPrefWidth(180);
                    ctrl.valueProperty().addListener((observable, oldValue, newValue) -> {
                        getMusicServiceSettings().setBackgroundColor(Utils.colorToString(newValue));
                        MusicHandler.INSTANCE.getPlayer().updateColor();
                    });
                }))
                .addControl("播放器背景不透明度", Utils.make(new JFXSlider(0.01, 1.0, getMusicServiceSettings().backgroundOpacity), slider -> {
                    slider.setPrefWidth(500);
                    slider.setBlockIncrement(0.01);
                    slider.setValueFactory(param -> Bindings.createStringBinding(() -> Math.round(slider.getValue() * 100) + "%", slider.valueProperty()));
                    slider.setStyle("-fx-font-family: '%s'".formatted(Settings.DEFAULT_FONT.getFamily()));
                    slider.setIndicatorPosition(JFXSlider.IndicatorPosition.LEFT);
                    slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                        double round = Math.round(newValue.doubleValue() * 100) / 100.0;
                        slider.setValue(round);
                        getMusicServiceSettings().setBackgroundOpacity(round);
                        MusicHandler.INSTANCE.getPlayer().updateColor();
                    });
                }))
                .addControl("播放器文字颜色", Utils.make(new JFXColorPicker(Color.valueOf(getMusicServiceSettings().textColor)), ctrl -> {
                    ctrl.setStyle("-fx-font-family: '%s'; -fx-font-size: 20".formatted(Settings.DEFAULT_FONT.getFamily()));
                    ctrl.setPrefHeight(40);
                    ctrl.setPrefWidth(180);
                    ctrl.valueProperty().addListener((observable, oldValue, newValue) -> {
                        getMusicServiceSettings().setTextColor(Utils.colorToString(newValue));
                        MusicHandler.INSTANCE.getPlayer().updateColor();
                    });
                }))
                .addControl("播放器文字字体", Utils.make(new JFXComboBox<String>(), comboBox -> {
                    comboBox.getItems().add("");
                    comboBox.getItems().addAll(FXCollections.observableArrayList(Font.getFamilies()));
                    comboBox.setValue(getMusicServiceSettings().textFont);
                    comboBox.setEditable(true);
                    comboBox.getEditor().setFont(Settings.DEFAULT_FONT);
                    comboBox.getStylesheets().add(Objects.requireNonNull(QueueManager.class.getResource("css/combobox.css")).toExternalForm());
                    comboBox.getStylesheets().add(Objects.requireNonNull(QueueManager.class.getResource("css/scrollbar.css")).toExternalForm());
                    comboBox.setPrefWidth(500);
                    comboBox.setCellFactory(new Callback<>() {
                        @Override
                        public ListCell<String> call(ListView<String> param) {
                            return new ListCell<>() {
                                @Override
                                protected void updateItem(String item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (item != null && !empty) {
                                        this.setFont(Settings.DEFAULT_FONT);
                                        this.setText(item.isEmpty() ? "默认字体" : item);
                                    }
                                }
                            };
                        }
                    });
                    comboBox.setButtonCell(Utils.make(new ListCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                this.setText(item.isEmpty() ? "默认字体" : item);
                            }
                        }
                    }, cell -> cell.setFont(Settings.DEFAULT_FONT)));
                    comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue != null) {
                            if (newValue.equals("默认字体")) {
                                newValue = "";
                            }
                            getMusicServiceSettings().setTextFont(newValue);
                            MusicHandler.INSTANCE.getPlayer().updateFont();
                        }
                    });
                }))
                .addControl("显示歌词", Utils.make(new JFXToggleButton(), button -> {
                    button.setSize(8);
                    button.setSelected(getMusicServiceSettings().displayLyric);
                    button.setDisableVisualFocus(true);
                    button.setOnAction(event -> {
                        getMusicServiceSettings().setDisplayLyric(button.isSelected());
                        MusicHandler.INSTANCE.getPlayer().setLyricShow();
                    });
                }))
                .addControl("显示点歌列表", Utils.make(new JFXToggleButton(), button -> {
                    button.setSize(8);
                    button.setSelected(getMusicServiceSettings().displayList);
                    button.setDisableVisualFocus(true);
                    button.setOnAction(event -> {
                        getMusicServiceSettings().setDisplayList(button.isSelected());
                        MusicHandler.INSTANCE.getPlayer().setQueueShow();
                    });
                }))
                .addPage()
                .addControl("点歌列表高度", Utils.make(new IntegerModifier(getMusicServiceSettings().listHeight, 20, 100, 1000), control -> {
                    control.setOnValueSet(i -> {
                        getMusicServiceSettings().setListHeight(i);
                        MusicHandler.INSTANCE.getPlayer().updateListHeight();
                        MusicHandler.INSTANCE.getPlayer().setQueueShow();
                    });
                }))
                .addControl("显示底部提示", Utils.make(new JFXToggleButton(), button -> {
                    button.setSize(8);
                    button.setSelected(getMusicServiceSettings().displayHint);
                    button.setDisableVisualFocus(true);
                    button.setOnAction(event -> getMusicServiceSettings().setDisplayHint(button.isSelected()));
                }))
                .addControl("空闲时歌单", Utils.make(new JFXComboBox<Pair<String, String>>(), comboBox -> {
                    comboBox.setDisable(true);
                    comboBox.getItems().add(new Pair<>("0", "无"));
                    comboBox.setFocusColor(Paint.valueOf("#1f1e33"));
                    comboBox.getStylesheets().add(Objects.requireNonNull(QueueManager.class.getResource("css/combobox.css")).toExternalForm());
                    comboBox.getStylesheets().add(Objects.requireNonNull(QueueManager.class.getResource("css/scrollbar.css")).toExternalForm());
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            JsonObject user = Music163Api.userInfo();
                            String userId = user.getAsJsonObject("profile").get("userId").getAsString();
                            JsonObject playlists = Music163Api.playLists(userId);
                            List<Pair<String, String>> list = new ArrayList<>();
                            playlists.getAsJsonArray("playlist").forEach(playlist -> {
                                JsonObject pl = playlist.getAsJsonObject();
                                list.add(new Pair<>(pl.get("id").getAsString(), pl.get("name").getAsString()));
                            });
                            return list;
                        } catch (Exception e) {
                            Platform.runLater(() -> Utils.showDialogMessage("获取用户歌单信息失败", true, QueueManager.INSTANCE.getMainScene().getRootDrawer()));
                            throw new RuntimeException(e);
                        }
                    }).whenComplete((pl, throwable) -> Platform.runLater(() -> {
                        if (throwable == null) {
                            comboBox.setDisable(false);
                            comboBox.getItems().addAll(pl);
                            Optional<Pair<String, String>> optional = comboBox.getItems().stream().filter(pl1 -> pl1.getKey().equals(getMusicServiceSettings().defaultPlaylist)).findFirst();
                            optional.ifPresent(comboBox::setValue);
                        }
                    }));
                    comboBox.setPrefWidth(500);
                    comboBox.setCellFactory(new Callback<>() {
                        @Override
                        public ListCell<Pair<String, String>> call(ListView<Pair<String, String>> param) {
                            return new ListCell<>() {
                                @Override
                                protected void updateItem(Pair<String, String> item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (item != null && !empty) {
                                        this.setFont(Settings.DEFAULT_FONT);
                                        this.setText(item.getValue());
                                    }
                                }
                            };
                        }
                    });
                    comboBox.setButtonCell(Utils.make(new ListCell<>() {
                        @Override
                        protected void updateItem(Pair<String, String> item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                this.setText(item.getValue());
                            }
                        }
                    }, cell -> cell.setFont(Settings.DEFAULT_FONT)));
                    comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue != null && !newValue.getKey().equals(getMusicServiceSettings().defaultPlaylist)) {
                            getMusicServiceSettings().setDefaultPlaylist(newValue.getKey());
                            MusicHandler.INSTANCE.fillPlayList();
                            if (MusicHandler.INSTANCE.getCurrentSong() == null || MusicHandler.INSTANCE.isSelfPlaylist()) {
                                MusicHandler.INSTANCE.clear(true);
                                CompletableFuture.runAsync(() -> {
                                    MusicHandler.INSTANCE.clear(false);
                                    MusicHandler.INSTANCE.playNext();
                                });
                            }
                        }
                    });
                }))
                .addControl("播放音量", Utils.make(new JFXSlider(0, 1.0, getMusicServiceSettings().volume), slider -> {
                    slider.setPrefWidth(500);
                    slider.setBlockIncrement(0.01);
                    slider.setValueFactory(param -> Bindings.createStringBinding(() -> Math.round(slider.getValue() * 100) + "%", slider.valueProperty()));
                    slider.setStyle("-fx-font-family: '%s'".formatted(Settings.DEFAULT_FONT.getFamily()));
                    slider.setIndicatorPosition(JFXSlider.IndicatorPosition.LEFT);
                    slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                        double round = Math.round(newValue.doubleValue() * 100) / 100.0;
                        slider.setValue(round);
                        getMusicServiceSettings().setVolume(round);
                    });
                }))
                .addControl("默认音质", Utils.make(new HBox(15), hBox -> {
                    ToggleGroup group = new ToggleGroup();
                    for (MusicServiceSettings.MusicLevel value : MusicServiceSettings.MusicLevel.values()) {
                        JFXRadioButton radioButton = new JFXRadioButton(value.getName());
                        radioButton.setAlignment(Pos.CENTER);
                        radioButton.setFont(Settings.DEFAULT_FONT);
                        radioButton.setSelected(getMusicServiceSettings().musicLevel == value);
                        radioButton.setOnAction(event -> getMusicServiceSettings().setMusicLevel(value));
                        radioButton.setToggleGroup(group);
                        if (value.ordinal() >= MusicServiceSettings.MusicLevel.LOSSLESS.ordinal()) {
                            Tooltip tooltip = new Tooltip("暂不支持");
                            tooltip.setShowDelay(Duration.ZERO);
                            tooltip.setHideDelay(Duration.ZERO);
                            tooltip.setFont(Settings.DEFAULT_FONT);
                            radioButton.setDisable(true);
                            radioButton.setTooltip(tooltip);
                        }
                        hBox.getChildren().add(radioButton);
                    }
                }))
                .addControl("歌单播放模式", Utils.make(new HBox(15), hBox -> {
                    ToggleGroup group = new ToggleGroup();
                    for (MusicServiceSettings.PlayMode value : MusicServiceSettings.PlayMode.values()) {
                        JFXRadioButton radioButton = new JFXRadioButton(value.getName());
                        radioButton.setAlignment(Pos.CENTER);
                        radioButton.setFont(Settings.DEFAULT_FONT);
                        radioButton.setSelected(getMusicServiceSettings().playMode == value);
                        radioButton.setOnAction(event -> getMusicServiceSettings().setPlayMode(value));
                        radioButton.setToggleGroup(group);
                        hBox.getChildren().add(radioButton);
                    }
                }))
                .addControl("启用“播放上一首”\n（调用历史记录播放上一首）", Utils.make(new JFXToggleButton(), button -> {
                    button.setSize(8);
                    button.setSelected(getMusicServiceSettings().playPrevious);
                    button.setDisableVisualFocus(true);
                    button.setOnAction(event -> {
                        getMusicServiceSettings().setPlayPrevious(button.isSelected());
                        MusicHandler.INSTANCE.getPlayer().refreshControlButtons(false);
                    });
                }))
                .addControl("提前下载模式\n（提前下载下一首歌，更快播放）", Utils.make(new JFXToggleButton(), button -> {
                    button.setSize(8);
                    button.setSelected(getMusicServiceSettings().earlyDownload);
                    button.setDisableVisualFocus(true);
                    button.setOnAction(event -> getMusicServiceSettings().setEarlyDownload(button.isSelected()));
                }))
                .build();
    }

    private static MusicServiceSettings getMusicServiceSettings() {
        return Settings.getMusicServiceSettings();
    }

    private Pane getC0() {
        return this.playerSettings == null ? this.playerSettings = initC0() : this.playerSettings;
    }

    private Pane initC1() {
        return new PagedContainerFactory("c1")
                .addNode(Utils.make(new BorderPane(), borderPane -> {
                    String name = "点歌功能";
                    String method1 = "+空格+歌名";
                    String method2 = "+空格+歌名@歌手";
                    String method3 = "#网易云歌曲ID";
                    Label label = Utils.createLabel("%s\n%s".formatted(name, "当前已关闭"), -1, Pos.CENTER_LEFT);
                    JFXTextField innerContainer = Utils.make(new JFXTextField(), control -> {
                        control.textProperty().addListener((observable, oldValue, newValue) -> {
                            getMusicServiceSettings().setRequestHeader(newValue);
                            newValue = newValue.length() > 5 ? newValue.substring(0, 5) + "…" : newValue;
                            if (newValue.isEmpty()) {
                                label.setText("%s\n%s".formatted(name, "当前已关闭"));
                            } else {
                                label.setText("%s\n用法1：%s%s\n用法2：%s%s\n用法3：%s%s".formatted(name, newValue, method1, newValue, method2, newValue, method3));
                            }
                        });
                        control.setPromptText("留空则关闭此功能");
                        control.setPrefWidth(300);
                        control.setText(getMusicServiceSettings().requestHeader);
                        control.setFont(Settings.DEFAULT_FONT);
                    });
                    borderPane.setLeft(label);
                    BorderPane.setAlignment(label, Pos.CENTER_LEFT);
                    borderPane.setRight(innerContainer);
                    BorderPane.setAlignment(innerContainer, Pos.CENTER_RIGHT);
                    borderPane.setBorder(new Border(DEFAULT_BORDER_STROKE));
                    borderPane.setPadding(new Insets(10, 10, 10, 10));
                }))
                .addNode(Utils.make(new BorderPane(), borderPane -> {
                    String name = "播放下一首功能";
                    String method = "";
                    Label label = Utils.createLabel("%s\n%s".formatted(name, "当前已关闭"), -1, Pos.CENTER_LEFT);
                    JFXTextField innerContainer = Utils.make(new JFXTextField(), control -> {
                        control.textProperty().addListener((observable, oldValue, newValue) -> {
                            getMusicServiceSettings().setSkipHeader(newValue);
                            newValue = newValue.length() > 5 ? newValue.substring(0, 5) + "…" : newValue;
                            if (newValue.isEmpty()) {
                                label.setText("%s\n%s".formatted(name, "当前已关闭"));
                            } else {
                                label.setText("%s\n用法：%s%s".formatted(name, newValue, method));
                            }
                        });
                        control.setPromptText("留空则关闭此功能");
                        control.setPrefWidth(300);
                        control.setText(getMusicServiceSettings().skipHeader);
                        control.setFont(Settings.DEFAULT_FONT);
                    });
                    borderPane.setLeft(label);
                    BorderPane.setAlignment(label, Pos.CENTER_LEFT);
                    borderPane.setRight(innerContainer);
                    BorderPane.setAlignment(innerContainer, Pos.CENTER_RIGHT);
                    borderPane.setBorder(new Border(DEFAULT_BORDER_STROKE));
                    borderPane.setPadding(new Insets(10, 10, 10, 10));
                }))
                .addNode(Utils.make(new BorderPane(), borderPane -> {
                    String name = "移除歌曲功能";
                    String method = "+空格+歌曲序号";
                    Label label = Utils.createLabel("%s\n%s".formatted(name, "当前已关闭"), -1, Pos.CENTER_LEFT);
                    JFXTextField innerContainer = Utils.make(new JFXTextField(), control -> {
                                control.textProperty().addListener((observable, oldValue, newValue) -> {
                                    getMusicServiceSettings().setRemoveHeader(newValue);
                                    newValue = newValue.length() > 5 ? newValue.substring(0, 5) + "…" : newValue;
                                    if (newValue.isEmpty()) {
                                        label.setText("%s\n%s".formatted(name, "当前已关闭"));
                                    } else {
                                        label.setText("%s\n用法：%s%s".formatted(name, newValue, method));
                                    }
                                });
                                control.setPromptText("留空则关闭此功能");
                                control.setPrefWidth(300);
                                control.setText(getMusicServiceSettings().removeHeader);
                                control.setFont(Settings.DEFAULT_FONT);
                            });
                    borderPane.setLeft(label);
                    BorderPane.setAlignment(label, Pos.CENTER_LEFT);
                    borderPane.setRight(innerContainer);
                    BorderPane.setAlignment(innerContainer, Pos.CENTER_RIGHT);
                    borderPane.setBorder(new Border(DEFAULT_BORDER_STROKE));
                    borderPane.setPadding(new Insets(10, 10, 10, 10));
                }))
                .addNode(Utils.make(new BorderPane(), borderPane -> {
                    String name = "置顶歌曲功能";
                    String method = "+空格+歌曲序号";
                    Label label = Utils.createLabel("%s\n%s".formatted(name, "当前已关闭"), -1, Pos.CENTER_LEFT);
                    JFXTextField innerContainer = Utils.make(new JFXTextField(), control -> {
                        control.textProperty().addListener((observable, oldValue, newValue) -> {
                            getMusicServiceSettings().setTopHeader(newValue);
                            newValue = newValue.length() > 5 ? newValue.substring(0, 5) + "…" : newValue;
                            if (newValue.isEmpty()) {
                                label.setText("%s\n%s".formatted(name, "当前已关闭"));
                            } else {
                                label.setText("%s\n用法：%s%s".formatted(name, newValue, method));
                            }
                        });
                        control.setPromptText("留空则关闭此功能");
                        control.setPrefWidth(300);
                        control.setText(getMusicServiceSettings().topHeader);
                        control.setFont(Settings.DEFAULT_FONT);
                    });
                    borderPane.setLeft(label);
                    BorderPane.setAlignment(label, Pos.CENTER_LEFT);
                    borderPane.setRight(innerContainer);
                    BorderPane.setAlignment(innerContainer, Pos.CENTER_RIGHT);
                    borderPane.setBorder(new Border(DEFAULT_BORDER_STROKE));
                    borderPane.setPadding(new Insets(10, 10, 10, 10));
                }))
                .addNode(Utils.make(new BorderPane(), borderPane -> {
                    String name = "立即播放歌曲功能";
                    String method = "+空格+歌曲序号";
                    Label label = Utils.createLabel("%s\n%s".formatted(name, "当前已关闭"), -1, Pos.CENTER_LEFT);
                    JFXTextField innerContainer = Utils.make(new JFXTextField(), control -> {
                        control.textProperty().addListener((observable, oldValue, newValue) -> {
                            getMusicServiceSettings().setPlayHeader(newValue);
                            newValue = newValue.length() > 5 ? newValue.substring(0, 5) + "…" : newValue;
                            if (newValue.isEmpty()) {
                                label.setText("%s\n%s".formatted(name, "当前已关闭"));
                            } else {
                                label.setText("%s\n用法：%s%s".formatted(name, newValue, method));
                            }
                        });
                        control.setPromptText("留空则关闭此功能");
                        control.setPrefWidth(300);
                        control.setText(getMusicServiceSettings().playHeader);
                        control.setFont(Settings.DEFAULT_FONT);
                    });
                    borderPane.setLeft(label);
                    BorderPane.setAlignment(label, Pos.CENTER_LEFT);
                    borderPane.setRight(innerContainer);
                    BorderPane.setAlignment(innerContainer, Pos.CENTER_RIGHT);
                    borderPane.setBorder(new Border(DEFAULT_BORDER_STROKE));
                    borderPane.setPadding(new Insets(10, 10, 10, 10));
                }))
                .addPage()
                .addControl("允许高级操作的用户组", Utils.make(new HBox(15), hBox -> {
                    for (MusicServiceSettings.UserGroups value : MusicServiceSettings.UserGroups.values()) {
                        JFXCheckBox checkBox = new JFXCheckBox(value.getName());
                        checkBox.setFont(Settings.DEFAULT_FONT);
                        checkBox.setSelected(getMusicServiceSettings().skipUsers.contains(value));
                        checkBox.setOnAction(event -> getMusicServiceSettings().modifySkipUsers(value, checkBox.isSelected()));
                        hBox.getChildren().add(checkBox);
                    }
                }))
                .addControl("点歌冷却/秒（0为无冷却）", Utils.make(new IntegerModifier(getMusicServiceSettings().requestCooldown, 10, 0, Integer.MAX_VALUE), control -> control.setOnValueSet(event -> getMusicServiceSettings().setRequestCooldown(control.getValue()))))
                .addControl("点歌队列上限/首（0为不设上限）", Utils.make(new IntegerModifier(getMusicServiceSettings().maxRequestCount, 1, 0, Integer.MAX_VALUE), control -> control.setOnValueSet(event -> getMusicServiceSettings().setMaxRequestCount(control.getValue()))))
                .addControl("允许无空格分隔点歌", Utils.make(new JFXToggleButton(), button -> {
                    button.setSize(8);
                    button.setSelected(getMusicServiceSettings().allowNoSpace);
                    button.setDisableVisualFocus(true);
                    button.setOnAction(event -> getMusicServiceSettings().setAllowNoSpace(button.isSelected()));
                }))
                .addControl("无点歌歌曲时立即播放", Utils.make(new JFXToggleButton(), button -> {
                    button.setSize(8);
                    button.setSelected(getMusicServiceSettings().instantPlay);
                    button.setDisableVisualFocus(true);
                    button.setOnAction(event -> getMusicServiceSettings().setInstantPlay(button.isSelected()));
                }))
                .build();
    }

    private Pane getC1() {
        return this.requestSettings == null ? this.requestSettings = initC1() : this.requestSettings;
    }

    private Pane initC2() {
        ObservableList<Song> songs = MusicHandler.INSTANCE.getHistory();
        return new MusicHistoryContainer("c2", songs).build();
    }

    private Pane getC2() {
        return this.history == null ? this.history = initC2() : this.history;
    }

    private Pane initC3() {
        return new PagedContainerFactory("c3")
                .addControl("账户名称", Utils.make(Utils.createLabel("加载中……"), label -> CompletableFuture.supplyAsync(() -> {
                    try {
                        JsonObject o = Music163Api.userInfo();
                        if (o.get("code").getAsInt() == 200) {
                            return o.getAsJsonObject("profile").get("nickname").getAsString();
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).whenComplete((success, throwable) -> Platform.runLater(() -> label.setText(Objects.requireNonNullElse(success, "获取失败"))))))
                .addControl("VIP 状态", Utils.make(Utils.createLabel("加载中……"), label -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String uid;
                        JsonObject o = Music163Api.userInfo();
                        if (o.get("code").getAsInt() == 200) {
                            uid = o.getAsJsonObject("profile").get("userId").getAsString();
                        } else {
                            return "获取失败";
                        }
                        o = Music163Api.vipInfo(uid);
                        if (o.get("code").getAsInt() == 200) {
                            long plusExpire = o.getAsJsonObject("data").getAsJsonObject("redplus").get("expireTime").getAsLong();
                            long commonExpire = o.getAsJsonObject("data").getAsJsonObject("associator").get("expireTime").getAsLong();
                            if (System.currentTimeMillis() < plusExpire) {
                                Instant instant = Instant.ofEpochMilli(plusExpire + 1);
                                LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA);
                                return "黑胶超会 - 过期时间：" + localDateTime.format(formatter);
                            }
                            if (System.currentTimeMillis() < commonExpire) {
                                Instant instant = Instant.ofEpochMilli(commonExpire + 1);
                                LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA);
                                return "黑胶会员 - 过期时间：" + localDateTime.format(formatter);
                            }
                            if (commonExpire > 0) {
                                Instant instant = Instant.ofEpochMilli(commonExpire + 1);
                                LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA);
                                return "会员已过期于：" + localDateTime.format(formatter);
                            }
                            return "未开通会员";
                        }
                        return "获取失败";
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).whenComplete((success, throwable) -> Platform.runLater(() -> label.setText(Objects.requireNonNullElse(success, "获取失败"))))))
                .addControl("避免播放 VIP 歌曲与版权歌曲", Utils.make(new JFXToggleButton(), button -> {
                    button.setSize(8);
                    button.setSelected(getMusicServiceSettings().noVipSongs);
                    button.setDisableVisualFocus(true);
                    button.setOnAction(event -> getMusicServiceSettings().setNoVipSongs(button.isSelected()));
                }))
                .addControl("退出登录", Utils.make(new QMButton("退出登录", "#fc3c55", false), button -> {
                    button.setGraphic(new WhiteFontIcon("fas-sign-out-alt"));
                    button.setPrefWidth(180);
                    button.setOnAction(event -> {
                        CompletableFuture.runAsync(() -> {
                            try {
                                Music163Api.logout();
                            } catch (Exception e) {
                                Platform.runLater(() -> Utils.showDialogMessage("登出失败", true, QueueManager.INSTANCE.getMainScene().getRootDrawer()));
                                button.disable(false);
                            }
                            MusicHandler.INSTANCE.clear();
                        }).whenComplete((result, throwable) -> Platform.runLater(() -> QueueManager.INSTANCE.getMainScene().setMainContainer(new MusicSystemPage().withParentPage(this.getParentPage()), this.getId())));
                        button.disable(true);
                    });
                }))
                .build();
    }

    private Pane getC3() {
        return this.accountSettings == null ? this.accountSettings = initC3() : this.accountSettings;
    }

    public void switchLoginButtonState() {
        if (!loginButton.isDisable()) {
            loginButton.disable(true);
            loginButton.setText("请在弹出窗口中完成登录");
            loginButton.setGraphic(new WhiteFontIcon("fas-bullseye"));
        }  else {
            loginButton.disable(false);
            loginButton.setText("点击登录网易云账号");
            loginButton.setGraphic(new WhiteFontIcon("fas-sign-in-alt"));
        }
    }

    @Override
    public LinkedHashMap<String, Supplier<Pane>> getMenus() {
        LinkedHashMap<String, Supplier<Pane>> map = new LinkedHashMap<>();
        if (!preloaded) {
            map.put("初始化", this::preload);
        } else if (!loggedIn) {
            map.put("账号登录", this::loginC0);
        } else {
            map.put("播放器", this::getC0);
            map.put("点歌机", this::getC1);
            map.put("播放历史", this::getC2);
            map.put("账号", this::getC3);
        }
        return map;
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
        return "点歌系统";
    }
}
