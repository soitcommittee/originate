package com.traceflow.notificationservice.services;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ErrorLogExtractor {
    private static final int MAX_LINES = 80;
    private static final int MAX_CHARACTERS = 8_000;
    private static final Pattern ANSI = Pattern.compile("\\u001B\\[[;\\d]*m");
    private static final Pattern KUBERNETES_TIMESTAMP = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z\\s+");
    private static final Pattern ERROR_START = Pattern.compile(
            "(?i)(?:\\blevel=(?:ERROR|FATAL)\\b|\\b(?:ERROR|FATAL)\\b|traceback \\(most recent call last\\))");
    private static final Pattern NEW_NON_ERROR_RECORD = Pattern.compile(
            "(?i)\\blevel=(?:TRACE|DEBUG|INFO|WARN)\\b");
    private static final Pattern STACK_SIGNAL = Pattern.compile(
            "(?i)(?:^\\s*at\\s+|^\\s*caused by:|^\\s*suppressed:|traceback \\(most recent call last\\)|"
                    + "\\b[\\w.$]+(?:Exception|Error)(?::|\\s|$))");

    public String extract(String rawLog) {
        if (rawLog == null || rawLog.isBlank()) return "";

        List<String> lines = rawLog.lines()
                .map(line -> ANSI.matcher(line).replaceAll(""))
                .map(line -> KUBERNETES_TIMESTAMP.matcher(line).replaceFirst(""))
                .toList();

        List<Block> blocks = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            if (!ERROR_START.matcher(lines.get(index)).find()) continue;
            int end = index + 1;
            boolean hasStack = STACK_SIGNAL.matcher(lines.get(index)).find();
            while (end < lines.size() && end - index < MAX_LINES) {
                String next = lines.get(end);
                if (NEW_NON_ERROR_RECORD.matcher(next).find() || ERROR_START.matcher(next).find()) break;
                hasStack |= STACK_SIGNAL.matcher(next).find();
                end++;
            }
            blocks.add(new Block(index, end, hasStack));
        }
        if (blocks.isEmpty()) return "";

        Block selected = blocks.stream().filter(Block::hasStack).reduce((first, second) -> second)
                .orElse(blocks.get(blocks.size() - 1));
        String excerpt = String.join("\n", lines.subList(selected.start(), selected.end())).trim();
        if (excerpt.length() <= MAX_CHARACTERS) return excerpt;
        return excerpt.substring(0, MAX_CHARACTERS) + "\n...[error excerpt truncated]";
    }

    private record Block(int start, int end, boolean hasStack) {
    }
}
