package com.school.leave.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.leave.auth.UserContext;
import com.school.leave.common.BizException;
import com.school.leave.common.enums.LeaveStatus;
import com.school.leave.common.enums.LeaveType;
import com.school.leave.leave.LeaveMapper;
import com.school.leave.leave.LeaveRequest;
import com.school.leave.user.SysUser;
import com.school.leave.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Spring AI 三能力（供应商优先级：OpenAI → Anthropic，均可自定义 base-url/model）：智能填单 / 审批建议 / 制度问答。
 * 未配置 Key 或调用异常 → 统一 5001 优雅降级，不影响主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 简明请假制度（chat 的 system 提示词） */
    private static final String POLICY = """
            你是某学院的学生请销假制度智能助手，请依据以下《学生请销假管理规定（简明版）》用简洁中文回答学生的问题，超出制度范围的问题请建议咨询辅导员：
            1. 请假1天（含）以内由辅导员直接审批；连续请假3天以上需同时报院系备案。
            2. 病假超过3天必须提供二级及以上医院的诊断证明或病历材料。
            3. 事假原则上每学期累计不超过7天，超出部分需提供充分理由。
            4. 突发急事可先电话向辅导员报备，返校后48小时内在系统补交请假申请。
            5. 请假期满应在返校当天通过系统申请销假，经辅导员确认后流程完结。
            6. 未经批准擅自离校或超假未销假的，按旷课处理并纳入综合测评。
            7. 考试周原则上不批准事假，病假需附医院证明。
            8. 请假离校期间须保持通讯畅通，如实填写目的地和联系电话。
            """;

    private final ObjectProvider<OpenAiChatModel> openAiChatModel;
    private final ObjectProvider<AnthropicChatModel> anthropicChatModel;
    private final LeaveMapper leaveMapper;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final com.school.leave.config.sys.ConfigService configService;
    private final AiChatLogMapper aiChatLogMapper;

    @Value("${spring.ai.openai.api-key:}")
    private String openAiKey;

    @Value("${spring.ai.anthropic.api-key:}")
    private String anthropicKey;

    /** openai | anthropic | auto（auto = 优先 OpenAI，其次 Anthropic） */
    @Value("${app.ai.provider:auto}")
    private String provider;

    private volatile ChatClient client;

    private static boolean realKey(String key) {
        return StringUtils.hasText(key) && !"sk-placeholder".equals(key.trim());
    }

    private boolean openAiReady() {
        return realKey(openAiKey) && openAiChatModel.getIfAvailable() != null;
    }

    private boolean anthropicReady() {
        return realKey(anthropicKey) && anthropicChatModel.getIfAvailable() != null;
    }

    /** 按供应商配置选择 ChatModel：显式指定 > auto（OpenAI 优先，回退 Anthropic） */
    private ChatModel selectModel() {
        return switch (provider == null ? "auto" : provider.trim().toLowerCase()) {
            case "openai" -> openAiReady() ? openAiChatModel.getIfAvailable() : null;
            case "anthropic" -> anthropicReady() ? anthropicChatModel.getIfAvailable() : null;
            default -> openAiReady() ? openAiChatModel.getIfAvailable()
                    : (anthropicReady() ? anthropicChatModel.getIfAvailable() : null);
        };
    }

    private boolean available() {
        return selectModel() != null;
    }

    private ChatClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    ChatModel model = selectModel();
                    if (model == null) throw BizException.aiUnavailable();
                    log.info("AI provider selected: {}", model.getClass().getSimpleName());
                    client = ChatClient.builder(model).build();
                }
            }
        }
        return client;
    }

    private String call(String system, String user) {
        if (!available()) throw BizException.aiUnavailable();
        try {
            String content = client().prompt().system(system).user(user).call().content();
            if (!StringUtils.hasText(content)) throw new IllegalStateException("empty ai response");
            return content;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI 调用失败: {}", e.toString());
            throw BizException.aiUnavailable();
        }
    }

    /** 智能填单：自然语言 → 请假单草稿 JSON（注入当天日期） */
    public Map<String, Object> draft(String text) {
        if (!StringUtils.hasText(text)) throw BizException.badParam("text 不能为空");
        LocalDate today = LocalDate.now();
        String week = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.SIMPLIFIED_CHINESE);
        String system = """
                你是学生请假单填写助手。今天是 %s（%s）。
                根据学生的自然语言描述生成请假单草稿。只输出一个 JSON 对象，不要输出任何其他文字或 markdown 代码块，格式如下：
                {"type":"SICK|PERSONAL|EMERGENCY|OTHER","startTime":"yyyy-MM-dd HH:mm:ss","endTime":"yyyy-MM-dd HH:mm:ss","reason":"简明事由(5~50字)","destination":"目的地，未提及则为空字符串"}
                规则：病假=SICK，事假=PERSONAL，急事=EMERGENCY，其他=OTHER；日期必须以今天为基准推算（如"下周一"）；未明确具体时刻时开始时间取 08:00:00、结束时间取 18:00:00；endTime 必须晚于 startTime。
                """.formatted(today.format(DateTimeFormatter.ISO_LOCAL_DATE), week);
        Map<String, Object> parsed = parseJson(call(system, text));

        // 解析校验
        String type = String.valueOf(parsed.get("type"));
        if (!LeaveType.isValid(type)) throw BizException.aiUnavailable();
        try {
            LocalDateTime.parse(String.valueOf(parsed.get("startTime")), DTF);
            LocalDateTime.parse(String.valueOf(parsed.get("endTime")), DTF);
        } catch (Exception e) {
            throw BizException.aiUnavailable();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("startTime", parsed.get("startTime"));
        data.put("endTime", parsed.get("endTime"));
        data.put("reason", parsed.getOrDefault("reason", ""));
        data.put("destination", parsed.getOrDefault("destination", ""));
        return data;
    }

    /** 审批建议：本单信息 + 该生近半年请假统计 */
    public Map<String, Object> approvalAdvice(LeaveRequest lr) {
        SysUser student = userMapper.enrichById(lr.getStudentId());
        LocalDateTime since = LocalDateTime.now().minusMonths(6);
        long recentCount = leaveMapper.countRecentByStudent(lr.getStudentId(), since);
        BigDecimal recentDays = leaveMapper.sumRecentDaysByStudent(lr.getStudentId(), since);
        long rejectedCount = leaveMapper.countRecentRejectedByStudent(lr.getStudentId(), since);

        String system = """
                你是高校辅导员的请假审批助手。请结合本次请假单信息与该生近半年请假统计，评估风险并给出审批建议。
                只输出一个 JSON 对象，不要任何其他文字或 markdown 代码块，格式：
                {"riskLevel":"LOW|MEDIUM|HIGH","advice":"一句话审批建议，60字以内，中文"}
                """;
        String user = """
                【本次请假单】学生：%s（%s，%s）；类型：%s；时长：%s天；起止：%s ~ %s；事由：%s；目的地：%s
                【该生近半年统计】请假申请 %d 次；获批请假累计 %s 天；被驳回 %d 次。
                """.formatted(
                student == null ? "未知" : student.getRealName(),
                student == null ? "-" : student.getStudentNo(),
                student == null ? "-" : student.getClassName(),
                LeaveType.textOf(lr.getType()), lr.getDays(),
                lr.getStartTime().format(DTF), lr.getEndTime().format(DTF),
                lr.getReason(), lr.getDestination() == null ? "-" : lr.getDestination(),
                recentCount, recentDays, rejectedCount);

        Map<String, Object> parsed = parseJson(call(system, user));
        String risk = String.valueOf(parsed.get("riskLevel"));
        if (!"LOW".equals(risk) && !"MEDIUM".equals(risk) && !"HIGH".equals(risk)) risk = "MEDIUM";
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("riskLevel", risk);
        data.put("advice", parsed.getOrDefault("advice", ""));
        return data;
    }

    /** 制度问答 */
    public Map<String, Object> chat(String message) {
        if (!StringUtils.hasText(message)) throw BizException.badParam("message 不能为空");
        String reply = call(policy(), message);
        String answer = reply.trim();
        logChat(message, answer);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reply", answer);
        return data;
    }

    /** 请假制度 system 提示词：优先读 sys_config('leave.policy')，读不到回退常量 */
    private String policy() {
        String cfg = configService.get("leave.policy");
        return StringUtils.hasText(cfg) ? cfg : POLICY;
    }

    /** best-effort 记录 AI 对话，失败绝不影响接口返回 */
    private void logChat(String question, String answer) {
        try {
            AiChatLog log = new AiChatLog();
            log.setUserId(UserContext.id());
            log.setQuestion(question);
            log.setAnswer(answer);
            log.setProvider(currentProvider());
            log.setCreateTime(LocalDateTime.now());
            aiChatLogMapper.insert(log);
        } catch (Exception e) {
            AiService.log.warn("AI 对话记录落库失败: {}", e.toString());
        }
    }

    /** 当前实际使用的供应商标签 */
    private String currentProvider() {
        ChatModel m = selectModel();
        if (m == null) return null;
        String cls = m.getClass().getSimpleName();
        if (cls.startsWith("OpenAi")) return "openai";
        if (cls.startsWith("Anthropic")) return "anthropic";
        return cls;
    }

    /** 从模型输出中截取并解析 JSON 对象 */
    private Map<String, Object> parseJson(String content) {
        try {
            int i = content.indexOf('{');
            int j = content.lastIndexOf('}');
            if (i < 0 || j <= i) throw new IllegalStateException("no json in ai response");
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(content.substring(i, j + 1), Map.class);
            return map;
        } catch (Exception e) {
            log.warn("AI 输出解析失败: {}", e.toString());
            throw BizException.aiUnavailable();
        }
    }

    /** 状态文案工具（供提示词使用） */
    @SuppressWarnings("unused")
    private String statusText(String status) {
        return LeaveStatus.textOf(status);
    }
}
