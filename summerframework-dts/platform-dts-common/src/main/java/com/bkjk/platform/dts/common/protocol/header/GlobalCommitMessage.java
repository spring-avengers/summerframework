
package com.bkjk.platform.dts.common.protocol.header;

import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class GlobalCommitMessage implements CommandCustomHeader, RequestMessage {

    @CFNotNull
    private long tranId;

    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public long getTranId() {
        return tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }
}
