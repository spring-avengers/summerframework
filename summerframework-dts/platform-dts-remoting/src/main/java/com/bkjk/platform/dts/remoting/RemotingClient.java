/**
 * Copyright (C) 2010-2013 Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.bkjk.platform.dts.remoting;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.bkjk.platform.dts.remoting.exception.RemotingConnectException;
import com.bkjk.platform.dts.remoting.exception.RemotingSendRequestException;
import com.bkjk.platform.dts.remoting.exception.RemotingTimeoutException;
import com.bkjk.platform.dts.remoting.exception.RemotingTooMuchRequestException;
import com.bkjk.platform.dts.remoting.netty.NettyRequestProcessor;
import com.bkjk.platform.dts.remoting.protocol.RemotingCommand;

/**
 * 远程通信，Client接口
 * 
 * @author shijia.wxr<vintage.wang@gmail.com>
 * @since 2013-7-13
 */
public interface RemotingClient extends RemotingService {

    public List<String> getNameServerAddressList();

    public void invokeAsync(final String addr, final RemotingCommand request, final long timeoutMillis,
        final InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException,
        RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    public void invokeOneway(final String addr, final RemotingCommand request, final long timeoutMillis)
        throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException,
        RemotingTimeoutException, RemotingSendRequestException;

    public RemotingCommand invokeSync(final String addr, final RemotingCommand request, final long timeoutMillis)
        throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException;

    public boolean isChannelWriteable(final String addr);

    public void registerProcessor(final int requestCode, final NettyRequestProcessor processor,
        final ExecutorService executor);

    public void updateNameServerAddressList(final List<String> addrs);
}
