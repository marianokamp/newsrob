package com.newsrob;

public class LoginWithCaptchaRequiredException extends AuthenticationFailedException {
	String captchaToken;
	String captchaUrl;

	public String getCaptchaToken() {
		return captchaToken;
	}

	public String getCaptchaUrl() {
		return captchaUrl;
	}

	LoginWithCaptchaRequiredException(String captchaToken, String captchaUrl) {
		super("CAPTCHA required");
		this.captchaToken = captchaToken;
		this.captchaUrl = captchaUrl;
	}

}
