package com.evallab.core.baseline;

public class BaselineNotFoundException extends RuntimeException {

    public BaselineNotFoundException(long baselineId) {
        super("基线不存在: " + baselineId);
    }
}
