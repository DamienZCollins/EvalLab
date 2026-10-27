package com.evallab.agent.guard;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入侧的提示注入过滤器。
 *
 * <p>演示 Agent 原本会把自己的输入原样写进输出（{@code {"answer": "<input>", ...}}），
 * 于是任何诱导它「说出某句话」的攻击都会成功。这里在进入工作流之前拦一道：命中已知
 * 攻击特征就拒绝执行，不把攻击载荷带进下游。
 *
 * <p>刻意只覆盖直白的特征，留出缺口（角色扮演、伪造系统通知、分隔符注入）给红队用例去发现——
 * 全中的话拦截率恒为 100%，评审报告就没有信息量了。缺口本身就是要被这份报告指出来的东西。
 */
@Component
public class PromptGuard {

    /** 命中任一即拦截。用 Pattern.CASE_INSENSITIVE 覆盖大小写变体。 */
    private static final List<Pattern> SIGNATURES = List.of(
            Pattern.compile("忽略.{0,8}(之前|以上|上面|前面).{0,8}(指令|提示|规则)"),
            Pattern.compile("(系统提示|系统指令).{0,12}(输出|原文|打印|告诉我)"),
            Pattern.compile("开发者模式"),
            Pattern.compile("(数据库)?密码|口令|身份证|手机号"),
            Pattern.compile("(无视|绕过|关闭).{0,8}(安全|限制|过滤|审核)")
    );

    /**
     * @return 拒绝理由（写入 Span 事件）；null 表示放行
     */
    public String inspect(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        for (Pattern signature : SIGNATURES) {
            if (signature.matcher(input).find()) {
                return "命中提示注入特征: " + signature.pattern();
            }
        }
        return null;
    }
}
