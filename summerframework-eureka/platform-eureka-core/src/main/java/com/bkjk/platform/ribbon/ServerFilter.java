package com.bkjk.platform.ribbon;

import java.util.List;

import com.netflix.loadbalancer.Server;

public interface ServerFilter {

    List<Server> match(List<Server> servers);
}
