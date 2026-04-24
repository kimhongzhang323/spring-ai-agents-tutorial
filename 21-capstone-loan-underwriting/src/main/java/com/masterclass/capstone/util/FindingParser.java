package com.masterclass.capstone.util;

import com.masterclass.capstone.domain.Finding;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON parser for the agent-produced findings array.
 * Uses regex extraction so the capstone has no Jackson dependency in agent classes.
 * Robust enough for well-formed LLM output; falls back gracefully on malformed JSON.
 */
public final class FindingParser {

    private static final Pattern ID_PAT = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SRC_PAT = Pattern.compile("\"source\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern STMT_PAT = Pattern.compile("\"statement\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern SEV_PAT = Pattern.compile("\"severity\"\\s*:\\s*\"([^\"]+)\"");
    // Split on object boundaries — each { ... } is one finding
    private static final Pattern OBJ_PAT = Pattern.compile("\\{[^}]+}");

    private FindingParser() {}

    public static List<Finding> parse(String json) {
        List<Finding> result = new ArrayList<>();
        Matcher objs = OBJ_PAT.matcher(json);
        while (objs.find()) {
            String obj = objs.group();
            String id = first(ID_PAT, obj, "XX-000");
            String src = first(SRC_PAT, obj, "UNKNOWN");
            String stmt = first(STMT_PAT, obj, obj);
            String sevStr = first(SEV_PAT, obj, "INFO");
            Finding.Severity sev;
            try {
                sev = Finding.Severity.valueOf(sevStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                sev = Finding.Severity.INFO;
            }
            result.add(new Finding(id, src, stmt, sev));
        }
        return result;
    }

    private static String first(Pattern p, String text, String fallback) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : fallback;
    }
}
