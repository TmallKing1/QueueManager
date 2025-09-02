package top.pigest.queuemanagerdemo.util;

import javafx.geometry.Point2D;
import javafx.scene.Node;

public class MouseUtils {
    /**
     * 检查鼠标是否在节点内（基于场景坐标）
     */
    public static boolean isMouseInsideNode(Node node, double sceneX, double sceneY) {
        Point2D nodeCoords = node.sceneToLocal(sceneX, sceneY);
        return node.contains(nodeCoords);
    }
}
