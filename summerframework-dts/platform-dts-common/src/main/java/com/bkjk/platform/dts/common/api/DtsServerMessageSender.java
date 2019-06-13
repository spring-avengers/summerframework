package com.bkjk.platform.dts.common.api;

import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platform.dts.common.protocol.RequestMessage;

public interface DtsServerMessageSender extends BaseMessageSender {

    public void invokeAsync(String clientAddress, RequestMessage msg) throws DtsException;

    public <T> T invokeSync(String clientAddress, RequestMessage msg) throws DtsException;

}
