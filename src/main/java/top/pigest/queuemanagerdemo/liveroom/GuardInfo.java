package top.pigest.queuemanagerdemo.liveroom;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * 舰长信息，不包含粉丝牌等级
 * @see LiveRoomApi#getGuardsWithExpireDate(int)
 */
public class GuardInfo {
    private final long uid;
    private final GuardType guardType;
    private final String name;
    private LocalDate expiredTime;
    public GuardInfo(long uid, GuardType guardType, String name) {
        this.uid = uid;
        this.guardType = guardType;
        this.name = name;
    }

    public GuardInfo(long uid, GuardType guardType, String name, LocalDate expiredTime) {
        this(uid, guardType, name);
        this.expiredTime = expiredTime;
    }

    public long getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public GuardType getGuardType() {
        return guardType;
    }

    public LocalDate getExpiredTime() {
        return expiredTime;
    }

    public String getExpiredTimeString() {
        if (expiredTime == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA);
        return expiredTime.format(formatter);
    }

    public int getDaysUntilExpire() {
        if (expiredTime == null) {
            return 0;
        }
        LocalDate now = LocalDate.now();
        return Math.toIntExact(ChronoUnit.DAYS.between(now, expiredTime));
    }
}
