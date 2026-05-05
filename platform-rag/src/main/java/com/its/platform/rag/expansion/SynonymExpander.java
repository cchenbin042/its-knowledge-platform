package com.its.platform.rag.expansion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synonym-based query expansion.
 * Expands query terms with predefined synonyms to improve recall.
 */
@Slf4j
@Component
public class SynonymExpander {

    // IT domain synonym dictionary
    private static final Map<String, List<String>> SYNONYM_DICT = new HashMap<>();

    static {
        // Network terms
        SYNONYM_DICT.put("网络", List.of("network", "networking", "net"));
        SYNONYM_DICT.put("服务器", List.of("server", "主机", "host"));
        SYNONYM_DICT.put("数据库", List.of("database", "db", "DB"));
        SYNONYM_DICT.put("防火墙", List.of("firewall", "FW"));
        SYNONYM_DICT.put("路由器", List.of("router", "路由"));
        SYNONYM_DICT.put("交换机", List.of("switch", "交换"));

        // Common IT terms
        SYNONYM_DICT.put("故障", List.of("error", "故障排查", "troubleshoot", "issue", "问题"));
        SYNONYM_DICT.put("配置", List.of("config", "configuration", "设置", "setup"));
        SYNONYM_DICT.put("部署", List.of("deploy", "deployment", "发布", "release"));
        SYNONYM_DICT.put("监控", List.of("monitor", "monitoring", "observability"));
        SYNONYM_DICT.put("日志", List.of("log", "logging", "日志分析"));
        SYNONYM_DICT.put("备份", List.of("backup", "恢复", "restore"));
        SYNONYM_DICT.put("安全", List.of("security", "sec", "信息安全"));
        SYNONYM_DICT.put("性能", List.of("performance", "perf", "优化", "optimize"));
        SYNONYM_DICT.put("虚拟化", List.of("virtualization", "VM", "虚拟机"));
        SYNONYM_DICT.put("容器", List.of("container", "docker", "k8s", "kubernetes"));
        SYNONYM_DICT.put("API", List.of("接口", "interface", "endpoint"));
        SYNONYM_DICT.put("认证", List.of("auth", "authentication", "登录", "login"));
        SYNONYM_DICT.put("权限", List.of("permission", "权限管理", "authorization", "RBAC"));
        SYNONYM_DICT.put("域名", List.of("domain", "DNS", "域名解析"));
        SYNONYM_DICT.put("IP", List.of("ip地址", "ip address", "地址"));
        SYNONYM_DICT.put("端口", List.of("port", "端口号"));
        SYNONYM_DICT.put("协议", List.of("protocol", "通信协议"));
        SYNONYM_DICT.put("CPU", List.of("cpu", "处理器", "processor"));
        SYNONYM_DICT.put("内存", List.of("memory", "RAM", "mem"));
        SYNONYM_DICT.put("磁盘", List.of("disk", "硬盘", "storage", "存储"));
        SYNONYM_DICT.put("带宽", List.of("bandwidth", "流量", "traffic"));
        SYNONYM_DICT.put("延迟", List.of("latency", "延时", "响应时间", "response time"));
        SYNONYM_DICT.put("负载均衡", List.of("load balance", "LB", "负载"));
        SYNONYM_DICT.put("缓存", List.of("cache", "caching", "redis"));
        SYNONYM_DICT.put("脚本", List.of("script", "自动化", "automation"));
        SYNONYM_DICT.put("版本", List.of("version", "ver", "版本控制"));
        SYNONYM_DICT.put("更新", List.of("update", "upgrade", "升级"));
        SYNONYM_DICT.put("安装", List.of("install", "installation", "部署"));
    }

    /**
     * Expand query with synonyms.
     * Returns original query plus expanded variants.
     *
     * @param query original query
     * @return expanded queries including original
     */
    public List<String> expand(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<String> expandedQueries = new ArrayList<>();
        expandedQueries.add(query);  // Always include original

        // Find terms that can be expanded
        Set<String> expandedTerms = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : SYNONYM_DICT.entrySet()) {
            String term = entry.getKey();
            List<String> synonyms = entry.getValue();

            if (containsTerm(query, term)) {
                // Add synonym expansions
                for (String synonym : synonyms) {
                    if (!containsTerm(query, synonym)) {
                        expandedTerms.add(synonym);
                    }
                }
            }

            // Also check reverse: synonym -> original term
            for (String synonym : synonyms) {
                if (containsTerm(query, synonym) && !containsTerm(query, term)) {
                    expandedTerms.add(term);
                }
            }
        }

        // Generate expanded query variants
        if (!expandedTerms.isEmpty()) {
            // Add query with all expansions appended
            String expandedQuery = query + " " + String.join(" ", expandedTerms);
            expandedQueries.add(expandedQuery);

            // Add individual synonym queries (limit to avoid explosion)
            int maxVariants = 3;
            for (String term : expandedTerms) {
                if (expandedQueries.size() >= maxVariants + 1) {
                    break;
                }
                expandedQueries.add(query + " " + term);
            }
        }

        log.info("Query expansion: {} -> {} variants", query, expandedQueries.size());
        return expandedQueries;
    }

    private boolean containsTerm(String text, String term) {
        Pattern pattern = Pattern.compile(Pattern.quote(term), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }
}