package com.school.leave.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.leave.common.enums.LeaveStatus;
import com.school.leave.common.enums.LeaveType;
import com.school.leave.leave.LeaveMapper;
import com.school.leave.leave.LeaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理端统计 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final LeaveMapper leaveMapper;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalCount", leaveMapper.selectCount(null));
        data.put("pendingCount", countStatus("PENDING"));
        data.put("approvedCount", countStatus("APPROVED"));
        data.put("completedCount", countStatus("COMPLETED"));

        // 类型分布
        Map<String, Long> typeCnt = toCountMap(leaveMapper.countByType(), "type");
        List<Map<String, Object>> typeDist = new ArrayList<>();
        for (LeaveType t : LeaveType.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", t.name());
            m.put("name", t.getText());
            m.put("count", typeCnt.getOrDefault(t.name(), 0L));
            typeDist.add(m);
        }
        data.put("typeDist", typeDist);

        // 近6个月趋势（含当月）
        YearMonth now = YearMonth.now();
        DateTimeFormatter mf = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> monthCnt = toCountMap(
                leaveMapper.countByMonth(now.minusMonths(5).atDay(1).atStartOfDay()), "month");
        List<Map<String, Object>> monthTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            String month = now.minusMonths(i).format(mf);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", month);
            m.put("count", monthCnt.getOrDefault(month, 0L));
            monthTrend.add(m);
        }
        data.put("monthTrend", monthTrend);

        // 状态分布
        Map<String, Long> statusCnt = toCountMap(leaveMapper.countByStatus(), "status");
        List<Map<String, Object>> statusDist = new ArrayList<>();
        for (LeaveStatus s : LeaveStatus.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", s.name());
            m.put("name", s.getText());
            m.put("count", statusCnt.getOrDefault(s.name(), 0L));
            statusDist.add(m);
        }
        data.put("statusDist", statusDist);
        return data;
    }

    private long countStatus(String status) {
        Long c = leaveMapper.selectCount(new QueryWrapper<LeaveRequest>().eq("status", status));
        return c == null ? 0 : c;
    }

    private Map<String, Long> toCountMap(List<Map<String, Object>> rows, String keyField) {
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object k = row.get(keyField);
            Object v = row.get("cnt");
            if (k != null && v instanceof Number n) map.put(k.toString(), n.longValue());
        }
        return map;
    }
}
