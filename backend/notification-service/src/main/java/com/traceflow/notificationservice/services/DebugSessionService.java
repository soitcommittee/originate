package com.traceflow.notificationservice.services;

import com.traceflow.notificationservice.dto.PostmortemRequest;
import com.traceflow.notificationservice.dto.DebugSessionResponse;

public interface DebugSessionService {
    DebugSessionResponse create(PostmortemRequest request);
}
