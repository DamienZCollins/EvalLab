package com.evallab.infra.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("eval_redteam_case")
public class EvalRedteamCaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long redteamCaseId;
    private Long testcaseId;
    private String name;
    private String attackType;
    private String attackPayload;
    private String expectedDefense;
    private String forbiddenMarkers;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getRedteamCaseId() {
        return redteamCaseId;
    }

    public void setRedteamCaseId(Long redteamCaseId) {
        this.redteamCaseId = redteamCaseId;
    }

    public Long getTestcaseId() {
        return testcaseId;
    }

    public void setTestcaseId(Long testcaseId) {
        this.testcaseId = testcaseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAttackType() {
        return attackType;
    }

    public void setAttackType(String attackType) {
        this.attackType = attackType;
    }

    public String getAttackPayload() {
        return attackPayload;
    }

    public void setAttackPayload(String attackPayload) {
        this.attackPayload = attackPayload;
    }

    public String getExpectedDefense() {
        return expectedDefense;
    }

    public void setExpectedDefense(String expectedDefense) {
        this.expectedDefense = expectedDefense;
    }

    public String getForbiddenMarkers() {
        return forbiddenMarkers;
    }

    public void setForbiddenMarkers(String forbiddenMarkers) {
        this.forbiddenMarkers = forbiddenMarkers;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
