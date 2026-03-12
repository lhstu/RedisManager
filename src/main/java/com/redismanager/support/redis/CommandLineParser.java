package com.redismanager.support.redis;

import com.redismanager.domain.redis.CommandRequest;

import java.util.ArrayList;
import java.util.List;

public final class CommandLineParser {
    private CommandLineParser() {
    }

    public static CommandRequest parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("命令不能为空");
        }

        List<String> tokens = tokenize(input);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("命令不能为空");
        }

        String command = tokens.get(0);
        List<String> arguments = tokens.size() == 1 ? List.of() : List.copyOf(tokens.subList(1, tokens.size()));
        return new CommandRequest(command, arguments);
    }

    private static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean escaping = false;

        for (int index = 0; index < input.length(); index++) {
            char ch = input.charAt(index);

            if (escaping) {
                current.append(ch);
                escaping = false;
                continue;
            }

            if (ch == '\\') {
                escaping = true;
                continue;
            }

            if (ch == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                continue;
            }

            if (ch == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                continue;
            }

            if (Character.isWhitespace(ch) && !inSingleQuotes && !inDoubleQuotes) {
                flushToken(tokens, current);
                continue;
            }

            current.append(ch);
        }

        if (escaping) {
            current.append('\\');
        }
        if (inSingleQuotes || inDoubleQuotes) {
            throw new IllegalArgumentException("命令引号未闭合");
        }

        flushToken(tokens, current);
        return tokens;
    }

    private static void flushToken(List<String> tokens, StringBuilder current) {
        if (current.isEmpty()) {
            return;
        }
        tokens.add(current.toString());
        current.setLength(0);
    }
}
