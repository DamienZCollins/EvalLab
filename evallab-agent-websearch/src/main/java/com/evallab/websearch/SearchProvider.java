package com.evallab.websearch;

import java.util.List;

/**
 * 搜索能力端口。默认实现是本地索引，保证离线评测可复现；
 * 换成真实搜索 API 只需另写一个实现并注册为 Bean。
 */
public interface SearchProvider {

    List<SearchHit> search(String query, int limit);

    /** 抓取单条结果的正文。 */
    String fetch(SearchHit hit);

    /**
     * @param title   结果标题
     * @param snippet 摘要，用于排序后展示
     * @param url     结果地址
     */
    record SearchHit(String title, String snippet, String url, String body, double score) {
    }
}
