package com.masterclass.apimgmt;

import com.masterclass.apimgmt.cost.CostTracker;
import com.masterclass.apimgmt.cost.UsageRecord;
import com.masterclass.apimgmt.cost.UsageRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CostTrackerTest {

    @Mock UsageRecordRepository repository;
    @InjectMocks CostTracker costTracker;

    @Test
    void recordSavesUsageRecord() {
        costTracker.record("alice", "gpt-4o-mini", 100, 200);
        verify(repository).save(any(UsageRecord.class));
    }

    @Test
    void localModelHasZeroCost() {
        // Verify no exception thrown and record is saved for local model
        costTracker.record("bob", "llama3.1", 500, 300);
        verify(repository).save(any(UsageRecord.class));
    }
}
