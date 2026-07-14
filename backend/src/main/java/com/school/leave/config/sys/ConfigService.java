package com.school.leave.config.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 系统配置服务 */
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SysConfigMapper mapper;

    /** 查配置值，查不到返回 null（best-effort，异常也返回 null） */
    public String get(String key) {
        if (key == null) return null;
        try {
            SysConfigEntity c = mapper.selectOne(new QueryWrapper<SysConfigEntity>()
                    .eq("config_key", key).last("LIMIT 1"));
            return c == null ? null : c.getConfigValue();
        } catch (Exception e) {
            return null;
        }
    }

    public List<SysConfigEntity> list() {
        return mapper.selectList(new QueryWrapper<SysConfigEntity>().orderByAsc("id"));
    }

    /** 更新配置值，key 不存在则新建 */
    public void set(String key, String value) {
        SysConfigEntity c = mapper.selectOne(new QueryWrapper<SysConfigEntity>()
                .eq("config_key", key).last("LIMIT 1"));
        if (c == null) {
            c = new SysConfigEntity();
            c.setConfigKey(key);
            c.setConfigValue(value);
            mapper.insert(c);
        } else {
            c.setConfigValue(value);
            mapper.updateById(c);
        }
    }
}
