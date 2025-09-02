module top.pigest.queuemanagerdemo {
    requires javafx.controls;
    requires javafx.fxml;

//    requires org.controlsfx.controls;
//    requires com.dlsc.formsfx;
    requires com.jfoenix;
    requires java.desktop;
    requires java.net.http;
    requires java.security.jgss;
    requires jdk.crypto.ec;
    requires org.apache.httpcomponents.httpclient;
    requires com.google.gson;
    requires org.apache.httpcomponents.httpcore;
    requires javafx.web;
    requires jdk.jsobject;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome6;
    requires dec;
    requires com.google.protobuf;
    requires javafx.media;

    opens top.pigest.queuemanagerdemo to javafx.fxml;
    opens top.pigest.queuemanagerdemo.system to com.google.gson;
    exports top.pigest.queuemanagerdemo;
    exports top.pigest.queuemanagerdemo.system;
    exports top.pigest.queuemanagerdemo.settings;
    exports top.pigest.queuemanagerdemo.util;
    exports top.pigest.queuemanagerdemo.control;
    exports top.pigest.queuemanagerdemo.window.main;
    exports top.pigest.queuemanagerdemo.window.music;
    opens top.pigest.queuemanagerdemo.settings to com.google.gson;
    exports top.pigest.queuemanagerdemo.music;
    opens top.pigest.queuemanagerdemo.music to com.google.gson;
    exports top.pigest.queuemanagerdemo.liveroom;
    opens top.pigest.queuemanagerdemo.liveroom to com.google.gson;
}