package com.redismanager.service.connection;

import com.redismanager.domain.connection.ConnectionProfileDraft;

import java.util.concurrent.CompletionStage;

public interface ConnectionTestService {
    CompletionStage<ConnectionTestResult> test(ConnectionProfileDraft draft);
}
