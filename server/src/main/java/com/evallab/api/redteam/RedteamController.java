package com.evallab.api.redteam;

import com.evallab.api.common.Result;
import com.evallab.core.redteam.RedteamAttackType;
import com.evallab.core.redteam.RedteamCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Redteam", description = "红队攻击用例管理")
@RestController
@RequestMapping("/api/redteam")
public class RedteamController {

    private final RedteamCaseService service;
    private final RedteamCaseMapper mapper;

    public RedteamController(RedteamCaseService service, RedteamCaseMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(summary = "查询红队用例", description = "可按攻击类型筛选；每条用例同时落一条 redteam 分组的评测用例")
    @GetMapping
    public Result<List<RedteamCaseResponse>> list(
            @RequestParam(name = "attack_type", required = false) RedteamAttackType attackType) {
        return Result.ok(service.query(attackType).stream().map(mapper::toResponse).toList());
    }

    @Operation(summary = "查询红队用例详情")
    @GetMapping("/{id}")
    public Result<RedteamCaseResponse> detail(@PathVariable long id) {
        return Result.ok(mapper.toResponse(service.get(id)));
    }

    @Operation(summary = "创建红队用例",
            description = "禁用标记用于生成守卫正则：Agent 输出中出现任一标记即判定被攻破")
    @PostMapping
    public Result<RedteamCaseResponse> create(@Valid @RequestBody RedteamCaseRequest request) {
        return Result.ok(mapper.toResponse(service.create(mapper.toDraft(request))));
    }

    @Operation(summary = "更新红队用例", description = "底层用例 version 自增，历史报告仍指向旧版本")
    @PutMapping("/{id}")
    public Result<RedteamCaseResponse> update(@PathVariable long id,
                                              @Valid @RequestBody RedteamCaseRequest request) {
        return Result.ok(mapper.toResponse(service.update(id, mapper.toDraft(request))));
    }

    @Operation(summary = "删除红队用例", description = "底层评测用例一并删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable long id) {
        service.delete(id);
        return Result.ok(null);
    }
}
