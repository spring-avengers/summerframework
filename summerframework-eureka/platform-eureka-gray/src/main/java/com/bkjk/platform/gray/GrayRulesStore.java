package com.bkjk.platform.gray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.collections.list.UnmodifiableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class GrayRulesStore {
    public static final Logger logger = LoggerFactory.getLogger(GrayRulesStore.class);
    public static final String KEY_HASH = "hash";

    public static final String KEY_GRAY_RULES = "grayRules";
    public static final String KEY_GROOVY_SCRIPT = "groovyScript";
    public static final String KEY_HEADERS = "headers";
    public static final String KEY_GROOVY_PARAMETERS = "parameters";
    public static final String KEY_GROOVY_BODY = "body";
    public static final String KEY_GROOVY_ID = "groovyId";
    public static final List<String> EMPTY = UnmodifiableList.decorate(new ArrayList<>());

    private Map<String, Object> store = new ConcurrentHashMap<>();

    public List<String> findGroovyScript(String from, String to) {
        try {
            String key = String.format("%s,%s", from, to).toUpperCase();
            if (!this.store.containsKey(KEY_GRAY_RULES)) {
                return EMPTY;
            }
            Map rules = (Map)this.store.get(KEY_GRAY_RULES);

            if (!rules.containsKey(key)) {
                return EMPTY;
            }
            List<Map> groovyScripts = (List<Map>)rules.get(key);

            if (groovyScripts.size() == 0) {
                return EMPTY;
            }

            return groovyScripts.stream().map(s -> s.get(KEY_GROOVY_SCRIPT)).filter(s -> s != null)
                .map(s -> s.toString()).collect(Collectors.toList());
        } catch (Throwable e) {
            logger.error("Error while get KEY_GROOVY_SCRIPT from store {}", store);
            logger.error(e.getMessage(), e);
            return EMPTY;
        }
    }

    public List<String> findHeader(String from) {
        try {
            String key = String.format("%s,", from).toUpperCase();
            if (!this.store.containsKey(KEY_GRAY_RULES)) {
                return EMPTY;
            }
            Map rules = (Map)this.store.get(KEY_GRAY_RULES);

            Set<String> keySet = rules.keySet();
            List<String> findHeaderKey = keySet.stream().filter(s -> s.startsWith(key)).collect(Collectors.toList());
            if (findHeaderKey.size() == 0) {
                return EMPTY;
            }

            List<Map> groovyScripts =
                findHeaderKey.stream().map(k -> (List<Map>)rules.get(k)).reduce(new ArrayList<Map>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                });

            if (groovyScripts.size() == 0) {
                return EMPTY;
            }

            return groovyScripts.stream().map(s -> s.get(KEY_HEADERS)).filter(s -> s != null)
                .reduce(new ArrayList<String>(), (list, hs) -> {
                    if (hs instanceof List) {
                        list.addAll((List)hs);
                    } else {
                        list.addAll(Arrays.asList(hs.toString().split(",")).stream()
                            .filter(s -> !StringUtils.isEmpty(s)).collect(Collectors.toList()));
                    }
                    return list;
                }, (a, b) -> {
                    a.addAll(b);
                    return a;
                });
        } catch (Throwable e) {
            logger.error("Error while get KEY_HEADERS from store {}", store);
            logger.error(e.getMessage(), e);
            return EMPTY;
        }
    }

    public String getRulesHash() {
        if (!store.containsKey(KEY_HASH)) {
            return "";
        }
        return store.get(KEY_HASH).toString();
    }

    public Map getStore() {
        return store;
    }

    public void setRules(Map rules) {
        if (!rules.containsKey(KEY_HASH)) {
            throw new IllegalArgumentException("Rule must contains key: " + KEY_HASH);
        }
        if (!rules.containsKey(KEY_GRAY_RULES)) {
            throw new IllegalArgumentException("Rule must contains key: " + KEY_GRAY_RULES);
        }
        Map<String, List> grayRules = (Map<String, List>)rules.get(KEY_GRAY_RULES);
        Map<String, List> grayRulesToSave = new ConcurrentHashMap<>();

        Set<String> keys = grayRules.keySet();
        for (String key : keys) {
            grayRulesToSave.put(key.toUpperCase(), grayRules.get(key));
        }
        this.store.put(KEY_HASH, rules.get(KEY_HASH));
        this.store.put(KEY_GRAY_RULES, grayRulesToSave);
    }

}
