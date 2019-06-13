package com.bkjk.platform.dts.common.api;

import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platform.dts.common.protocol.RequestMessage;

public interface DtsClientMessageSender extends BaseMessageSender {

    public <T> T invoke(RequestMessage msg) throws DtsException;
}
