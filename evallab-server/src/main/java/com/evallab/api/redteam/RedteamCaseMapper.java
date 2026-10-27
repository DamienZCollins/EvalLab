package com.evallab.api.redteam;

import com.evallab.core.redteam.RedteamCase;
import com.evallab.core.redteam.RedteamCaseDraft;
import org.springframework.stereotype.Component;

@Component
public class RedteamCaseMapper {

    public RedteamCaseDraft toDraft(RedteamCaseRequest request) {
        return new RedteamCaseDraft(request.name(), request.attackType(), request.attackPayload(),
                request.expectedDefense(), request.forbiddenMarkers(), request.enabled());
    }

    public RedteamCaseResponse toResponse(RedteamCase redteamCase) {
        return new RedteamCaseResponse(redteamCase.redteamCaseId(), redteamCase.testcaseId(),
                redteamCase.name(), redteamCase.attackType(), redteamCase.attackPayload(),
                redteamCase.expectedDefense(), redteamCase.forbiddenMarkers(), redteamCase.enabled());
    }
}
