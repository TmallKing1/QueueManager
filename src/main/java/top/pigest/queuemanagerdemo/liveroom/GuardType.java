package top.pigest.queuemanagerdemo.liveroom;

public enum GuardType {
    NONE("无", 0),
    ZONG("总督", 1),
    TI("提督", 2),
    JIAN("舰长", 3);

    private final String name;
    private final int guardLevel;

    GuardType(String name, int guardLevel) {
        this.name = name;
        this.guardLevel = guardLevel;
    }

    public String getName() {
        return name;
    }

    public int getGuardLevel() {
        return guardLevel;
    }

    public boolean isGuard() {
        return guardLevel > 0;
    }

    public static GuardType valueOf(int value) {
        for (GuardType guardType : GuardType.values()) {
            if (guardType.getGuardLevel() == value) {
                return guardType;
            }
        }
        return NONE;
    }
}
