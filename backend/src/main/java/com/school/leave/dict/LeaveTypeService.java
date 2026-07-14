package com.school.leave.dict;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 请假类型字典服务 */
@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeMapper mapper;

    /** enabled=1，按 sort 升序 */
    public List<LeaveTypeEntity> listEnabled() {
        return mapper.selectList(new QueryWrapper<LeaveTypeEntity>()
                .eq("enabled", 1).orderByAsc("sort", "id"));
    }

    /** 校验 typeCode 是否为字典中启用的类型 */
    public boolean existsEnabled(String typeCode) {
        if (typeCode == null) return false;
        Long c = mapper.selectCount(new QueryWrapper<LeaveTypeEntity>()
                .eq("type_code", typeCode).eq("enabled", 1));
        return c != null && c > 0;
    }

    /** 取该类型单次最大天数；查不到返回 null（表示不限制） */
    public Integer maxDaysOf(String typeCode) {
        if (typeCode == null) return null;
        LeaveTypeEntity e = mapper.selectOne(new QueryWrapper<LeaveTypeEntity>()
                .eq("type_code", typeCode).last("LIMIT 1"));
        return e == null ? null : e.getMaxDays();
    }
}
