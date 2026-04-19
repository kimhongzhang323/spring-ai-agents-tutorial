package com.masterclass.providers.tuning;

import java.util.Map;

public record TemperatureComparisonResult(
        String prompt,
        String providerUsed,
        Map<String, String> responsesByTemperature
) {}
