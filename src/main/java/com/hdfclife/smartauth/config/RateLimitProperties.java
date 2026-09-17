package com.hdfclife.smartauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "login.rate-limit")
public class RateLimitProperties {

    private int limitForPeriod;
    private Duration limitRefreshPeriod;
    private Duration timeoutDuration;

    public int getLimitForPeriod() {
        return limitForPeriod;
    }

    public void setLimitForPeriod(int limitForPeriod) {
        this.limitForPeriod = limitForPeriod;
    }

    public Duration getLimitRefreshPeriod() {
        return limitRefreshPeriod;
    }

    public void setLimitRefreshPeriod(Duration limitRefreshPeriod) {
        this.limitRefreshPeriod = limitRefreshPeriod;
    }

    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }

    public void setTimeoutDuration(Duration timeoutDuration) {
        this.timeoutDuration = timeoutDuration;
    }
}
