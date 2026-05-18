package com.enterprise.timeout.alert.channel;

import com.enterprise.timeout.model.TimeoutEvent;

public interface AlertChannel {

    String getName();

    boolean send(String recipient, String message, TimeoutEvent event);
}
