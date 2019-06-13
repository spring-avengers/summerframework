package com.bkjk.platform.dts.remoting;

public interface RemotingService {
    public void registerRPCHook(RPCHook rpcHook);

    public void shutdown();

    public void start();
}
