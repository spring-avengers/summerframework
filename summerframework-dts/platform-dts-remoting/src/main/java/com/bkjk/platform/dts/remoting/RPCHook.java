package com.bkjk.platform.dts.remoting;

import com.bkjk.platform.dts.remoting.protocol.RemotingCommand;

public interface RPCHook {
    public void doAfterResponse(final String remoteAddr, final RemotingCommand request, final RemotingCommand response);

    public void doBeforeRequest(final String remoteAddr, final RemotingCommand request);
}
