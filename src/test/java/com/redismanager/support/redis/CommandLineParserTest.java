package com.redismanager.support.redis;

import com.redismanager.domain.redis.CommandRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandLineParserTest {
    @Test
    void shouldParseSimpleCommand() {
        CommandRequest request = CommandLineParser.parse("set user:1 hello");
        assertEquals("set", request.command());
        assertEquals(List.of("user:1", "hello"), request.arguments());
    }

    @Test
    void shouldParseQuotedArguments() {
        CommandRequest request = CommandLineParser.parse("set \"user name\" 'hello world'");
        assertEquals("set", request.command());
        assertEquals(List.of("user name", "hello world"), request.arguments());
    }

    @Test
    void shouldRejectUnclosedQuote() {
        assertThrows(IllegalArgumentException.class, () -> CommandLineParser.parse("set \"abc"));
    }
}
