package top.pigest.queuemanagerdemo.liveroom;

public enum GuardType {
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
}
