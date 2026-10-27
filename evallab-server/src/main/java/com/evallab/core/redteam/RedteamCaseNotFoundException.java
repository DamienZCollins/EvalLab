package com.evallab.core.redteam;

/** 红队用例不存在，映射为 404。 */
public class RedteamCaseNotFoundException extends RuntimeException {

    public RedteamCaseNotFoundException(long redteamCaseId) {
        super("红队用例不存在: " + redteamCaseId);
    }
}
