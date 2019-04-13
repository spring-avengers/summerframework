package com.bkjk.platform.gray;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.util.Assert;

public class GroovyScriptEngineUtil {

    public static final String CHOOSE = "choose";

    public static final ConcurrentHashMap<String, ScriptEngine> engineCache = new ConcurrentHashMap<>();

    public static Object executeChoose(String script, Object... args) throws ScriptException, NoSuchMethodException {
        ScriptEngine engine = engineCache.get(script);
        if (engine == null) {
            synchronized (engineCache) {
                if (!engineCache.contains(script)) {
                    ScriptEngineManager factory = new ScriptEngineManager();
                    engine = factory.getEngineByName("groovy");
                    engine.eval(script, engine.createBindings());
                    engineCache.put(script, engine);
                }
            }
        }
        Invocable invocable = (Invocable)engine;
        return invocable.invokeFunction(CHOOSE, args);
    }

    public static void main(String[] args) throws ScriptException, NoSuchMethodException, InterruptedException {

        String script = "def choose(request){def node=[:];node['version']='1';return node;}";
        Map request = new HashMap();
        System.out.println(executeChoose(script, request).getClass());
        System.out.println(executeChoose(script, request));
        test(10, 2000);
    }

    public static void test(int count, int total) throws ScriptException, NoSuchMethodException, InterruptedException {

        String script = "def choose(request){return request['x-bkjk-xx']+%s}";

        ExecutorService exec = Executors.newFixedThreadPool(count);

        while (total-- > 0) {

            exec.submit(new Runnable() {
                @Override
                public void run() {
                    int r = new Random().nextInt(10);
                    Map request = new HashMap();
                    request.put("x-bkjk-xx", "req" + r);
                    try {
                        Object ret = executeChoose(String.format(script, r), request);
                        Assert.isTrue(ret.equals("req" + r + r));
                    } catch (ScriptException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.DAYS);

    }
}
