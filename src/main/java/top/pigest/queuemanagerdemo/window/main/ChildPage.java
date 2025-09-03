package top.pigest.queuemanagerdemo.window.main;

import javafx.scene.layout.Pane;

public interface ChildPage {
    Pane getParentPage();
    void setParentPage(Pane parentPage);
}
