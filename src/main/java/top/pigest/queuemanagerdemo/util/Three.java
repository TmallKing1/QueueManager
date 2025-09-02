package top.pigest.queuemanagerdemo.util;

import java.util.Objects;

public class Three<A, B, C> {
    private A first;
    private B second;
    private C third;
    public Three(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public A getFirst() {
        return first;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public B getSecond() {
        return second;
    }

    public void setSecond(B second) {
        this.second = second;
    }

    public C getThird() {
        return third;
    }

    public void setThird(C third) {
        this.third = third;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Three<?, ?, ?> three)) return false;
        return Objects.equals(getFirst(), three.getFirst()) && Objects.equals(getSecond(), three.getSecond()) && Objects.equals(getThird(), three.getThird());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFirst(), getSecond(), getThird());
    }
}
