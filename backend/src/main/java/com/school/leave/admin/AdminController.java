package com.school.leave.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.leave.common.BizException;
import com.school.leave.common.PageVO;
import com.school.leave.common.Result;
import com.school.leave.config.RequireRole;
import com.school.leave.leave.LeaveService;
import com.school.leave.leave.LeaveVO;
import com.school.leave.user.SysUser;
import com.school.leave.user.SysUserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 管理端：用户 CRUD / 全部请假单 / 统计 */
@RestController
@RequestMapping("/admin")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class AdminController {

    private final SysUserMapper userMapper;
    private final LeaveService leaveService;
    private final StatsService statsService;
    private final com.school.leave.config.sys.ConfigService configService;

    private static final Set<String> ROLES = Set.of("STUDENT", "TEACHER", "ADMIN");

    @Data
    public static class ConfigDTO {
        private String value;
    }

    @Data
    public static class UserDTO {
        private String username;
        private String password;
        private String realName;
        private String role;
        private String studentNo;
        private String className;
        private String phone;
        private Long teacherId;
    }

    @Data
    public static class PasswordDTO {
        private String password;
    }

    // ---------------- 用户管理 ----------------

    @GetMapping("/users")
    public Result<PageVO<Map<String, Object>>> users(@RequestParam(required = false) String role,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        if (StringUtils.hasText(role)) qw.eq("role", role);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("username", keyword).or().like("real_name", keyword).or().like("student_no", keyword));
        }
        qw.orderByAsc("id");
        IPage<SysUser> p = userMapper.selectPage(new Page<>(page, size), qw);

        // 补 teacherName
        Set<Long> tids = p.getRecords().stream().map(SysUser::getTeacherId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> tNames = tids.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(tids).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));

        PageVO<Map<String, Object>> vo = new PageVO<>();
        vo.setTotal(p.getTotal());
        vo.setCurrent(p.getCurrent());
        vo.setSize(p.getSize());
        vo.setRecords(p.getRecords().stream().map((Function<SysUser, Map<String, Object>>) u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("role", u.getRole());
            m.put("studentNo", u.getStudentNo());
            m.put("className", u.getClassName());
            m.put("phone", u.getPhone());
            m.put("teacherId", u.getTeacherId());
            m.put("teacherName", u.getTeacherId() == null ? null : tNames.get(u.getTeacherId()));
            m.put("status", u.getStatus());
            m.put("createTime", u.getCreateTime());
            return m;
        }).collect(Collectors.toList()));
        return Result.ok(vo);
    }

    @PostMapping("/users")
    public Result<SysUser> createUser(@RequestBody UserDTO dto) {
        if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())
                || !StringUtils.hasText(dto.getRealName()) || !ROLES.contains(String.valueOf(dto.getRole()))) {
            throw BizException.badParam("username/password/realName/role 不合法");
        }
        if ("STUDENT".equals(dto.getRole()) && dto.getTeacherId() == null) {
            throw BizException.badParam("学生必须指定辅导员 teacherId");
        }
        if (dto.getTeacherId() != null) {
            SysUser t = userMapper.selectById(dto.getTeacherId());
            if (t == null || !"TEACHER".equals(t.getRole())) throw BizException.badParam("teacherId 不是有效辅导员");
        }
        Long exists = userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", dto.getUsername()));
        if (exists != null && exists > 0) throw BizException.badParam("用户名已存在");

        SysUser u = new SysUser();
        u.setUsername(dto.getUsername());
        u.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(10)));
        u.setRealName(dto.getRealName());
        u.setRole(dto.getRole());
        u.setStudentNo(dto.getStudentNo());
        u.setClassName(dto.getClassName());
        u.setPhone(dto.getPhone());
        u.setTeacherId("STUDENT".equals(dto.getRole()) ? dto.getTeacherId() : null);
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        userMapper.insert(u);
        return Result.ok(u);
    }

    @PutMapping("/users/{id}")
    public Result<SysUser> updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw BizException.notFound("用户不存在");
        if (StringUtils.hasText(dto.getRealName())) u.setRealName(dto.getRealName());
        if (dto.getRole() != null) {
            if (!ROLES.contains(dto.getRole())) throw BizException.badParam("role 不合法");
            u.setRole(dto.getRole());
        }
        if (dto.getStudentNo() != null) u.setStudentNo(dto.getStudentNo());
        if (dto.getClassName() != null) u.setClassName(dto.getClassName());
        if (dto.getPhone() != null) u.setPhone(dto.getPhone());
        if (dto.getTeacherId() != null) {
            SysUser t = userMapper.selectById(dto.getTeacherId());
            if (t == null || !"TEACHER".equals(t.getRole())) throw BizException.badParam("teacherId 不是有效辅导员");
            u.setTeacherId(dto.getTeacherId());
        }
        userMapper.updateById(u);
        return Result.ok(u);
    }

    @PutMapping("/users/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody PasswordDTO dto) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw BizException.notFound("用户不存在");
        if (!StringUtils.hasText(dto.getPassword()) || dto.getPassword().length() < 6) {
            throw BizException.badParam("密码不能少于6位");
        }
        u.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(10)));
        userMapper.updateById(u);
        return Result.ok();
    }

    /** 逻辑删除：status=0，不可登录 */
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw BizException.notFound("用户不存在");
        u.setStatus(0);
        userMapper.updateById(u);
        return Result.ok();
    }

    /** 辅导员下拉 */
    @GetMapping("/teachers")
    public Result<List<Map<String, Object>>> teachers() {
        List<SysUser> list = userMapper.selectList(new QueryWrapper<SysUser>()
                .eq("role", "TEACHER").eq("status", 1).orderByAsc("id"));
        List<Map<String, Object>> data = list.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", t.getId());
            m.put("realName", t.getRealName());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(data);
    }

    // ---------------- 全部请假单 / 统计 ----------------

    @GetMapping("/leaves")
    public Result<PageVO<LeaveVO>> leaves(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.ok(leaveService.pageAdmin(status, keyword, page, size));
    }

    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> statsOverview() {
        return Result.ok(statsService.overview());
    }

    // ---------------- 系统配置 ----------------

    @GetMapping("/configs")
    public Result<List<com.school.leave.config.sys.SysConfigEntity>> configs() {
        return Result.ok(configService.list());
    }

    @PutMapping("/configs/{key}")
    public Result<Void> updateConfig(@PathVariable String key, @RequestBody ConfigDTO dto) {
        if (!StringUtils.hasText(key)) throw BizException.badParam("配置键不能为空");
        configService.set(key, dto == null ? null : dto.getValue());
        return Result.ok();
    }
}
