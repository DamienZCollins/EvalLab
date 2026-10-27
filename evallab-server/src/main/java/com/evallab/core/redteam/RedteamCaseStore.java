package com.evallab.core.redteam;

import java.util.List;
import java.util.Optional;

/** 红队用例持久化端口。 */
public interface RedteamCaseStore {

    RedteamCase insert(long testcaseId, RedteamCaseDraft draft);

    RedteamCase update(long redteamCaseId, RedteamCaseDraft draft);

    void delete(long redteamCaseId);

    Optional<RedteamCase> findById(long redteamCaseId);

    /** @param attackType null 表示不筛类型 */
    List<RedteamCase> find(RedteamAttackType attackType);
}
