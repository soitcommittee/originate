package com.traceflow.loanservice.services;

import com.traceflow.loanservice.dto.responses.ApplicationStatusSnapshotResponse;

public interface ApplicationStatusSnapshotService {
    ApplicationStatusSnapshotResponse capture();
}
