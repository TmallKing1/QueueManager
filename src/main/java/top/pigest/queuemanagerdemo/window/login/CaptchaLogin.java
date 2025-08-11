package top.pigest.queuemanagerdemo.window.login;

public interface CaptchaLogin {
    void startCaptcha();
    void captchaSuccess(String token, String gt, String challenge, String validate, String seccode);
    void captchaFail(boolean manualCancel, String failMessage);
}
