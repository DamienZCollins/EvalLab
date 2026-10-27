package com.evallab.core.redteam;

import java.util.List;

/** 红队用例不合法，映射为 400。 */
public class InvalidRedteamCaseException extends RuntimeException {

    public InvalidRedteamCaseException(List<String> violations) {
        super(String.join("; ", violations));
    }
}
