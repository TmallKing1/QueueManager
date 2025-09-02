package top.pigest.queuemanagerdemo.util;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

public class MouseUtils {
    /**
     * 检查鼠标是否在节点内
     */
    public static boolean isMouseInsideNode(Node node, MouseEvent event) {
        Point2D nodeCoords = node.sceneToLocal(event.getSceneX(), event.getSceneY());
        return node.contains(nodeCoords);
    }

    /**
     * 检查鼠标是否在节点内（基于场景坐标）
     */
    public static boolean isMouseInsideNode(Node node, double sceneX, double sceneY) {
        Point2D nodeCoords = node.sceneToLocal(sceneX, sceneY);
        return node.contains(nodeCoords);
    }
}
