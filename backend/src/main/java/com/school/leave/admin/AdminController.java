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
import com.school.leave.user.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.transaction.annotation.Transactional;
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
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final ClassInfoMapper classInfoMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
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
        private Long teacherId;   // 辅导员的 sys_user.id（/admin/teachers 下拉口径）
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
        IPage<SysUser> p = userMapper.pageEnriched(new Page<>(page, size), role, keyword);

        // teacherName：学生的 teacherId=辅导员 sys_user.id → 取其（enrich 后的）realName
        Set<Long> tids = p.getRecords().stream().map(SysUser::getTeacherId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> tNames = new HashMap<>();
        for (Long tid : tids) {
            SysUser t = userMapper.enrichById(tid);
            if (t != null) tNames.put(tid, t.getRealName());
        }

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
    @Transactional
    public Result<SysUser> createUser(@RequestBody UserDTO dto) {
        if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())
                || !StringUtils.hasText(dto.getRealName()) || !ROLES.contains(String.valueOf(dto.getRole()))) {
            throw BizException.badParam("username/password/realName/role 不合法");
        }
        if ("STUDENT".equals(dto.getRole()) && dto.getTeacherId() == null) {
            throw BizException.badParam("学生必须指定辅导员 teacherId");
        }
        Teacher counselor = null;
        if (dto.getTeacherId() != null) {
            counselor = teacherByUserId(dto.getTeacherId());
            if (counselor == null) throw BizException.badParam("teacherId 不是有效辅导员");
        }
        Long exists = userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", dto.getUsername()));
        if (exists != null && exists > 0) throw BizException.badParam("用户名已存在");

        // 1) 账号
        SysUser u = new SysUser();
        u.setUsername(dto.getUsername());
        u.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(10)));
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        userMapper.insert(u);

        // 2) 角色关联
        Long roleId = roleIdOf(dto.getRole());
        SysUserRole ur = new SysUserRole();
        ur.setUserId(u.getId());
        ur.setRoleId(roleId);
        userRoleMapper.insert(ur);

        // 3) 身份档案
        if ("STUDENT".equals(dto.getRole())) {
            Student st = new Student();
            st.setUserId(u.getId());
            st.setStudentNo(StringUtils.hasText(dto.getStudentNo()) ? dto.getStudentNo() : dto.getUsername());
            st.setRealName(dto.getRealName());
            st.setPhone(dto.getPhone());
            st.setCounselorId(counselor.getId());
            st.setClassId(resolveClassId(dto.getClassName(), counselor.getId()));
            studentMapper.insert(st);
        } else if ("TEACHER".equals(dto.getRole())) {
            Teacher te = new Teacher();
            te.setUserId(u.getId());
            te.setTeacherNo(StringUtils.hasText(dto.getStudentNo()) ? dto.getStudentNo() : dto.getUsername());
            te.setRealName(dto.getRealName());
            te.setPhone(dto.getPhone());
            teacherMapper.insert(te);
        }
        // ADMIN 无档案表

        return Result.ok(userMapper.enrichById(u.getId()));
    }

    @PutMapping("/users/{id}")
    @Transactional
    public Result<SysUser> updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw BizException.notFound("用户不存在");
        String role = userRoleMapper.roleCodeOf(id);

        if (dto.getRole() != null && !ROLES.contains(dto.getRole())) {
            throw BizException.badParam("role 不合法");
        }
        // 角色变更：重建角色关联（保持“单角色”约束）
        if (dto.getRole() != null && !dto.getRole().equals(role)) {
            userRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", id));
            SysUserRole ur = new SysUserRole();
            ur.setUserId(id);
            ur.setRoleId(roleIdOf(dto.getRole()));
            userRoleMapper.insert(ur);
            role = dto.getRole();
        }

        // 更新对应身份档案
        Student st = studentByUserId(id);
        Teacher te = teacherByUserId(id);
        if ("STUDENT".equals(role) && st != null) {
            if (StringUtils.hasText(dto.getRealName())) st.setRealName(dto.getRealName());
            if (dto.getStudentNo() != null) st.setStudentNo(dto.getStudentNo());
            if (dto.getPhone() != null) st.setPhone(dto.getPhone());
            if (dto.getTeacherId() != null) {
                Teacher counselor = teacherByUserId(dto.getTeacherId());
                if (counselor == null) throw BizException.badParam("teacherId 不是有效辅导员");
                st.setCounselorId(counselor.getId());
                Long cid = resolveClassId(dto.getClassName(), counselor.getId());
                if (cid != null) st.setClassId(cid);
            } else if (dto.getClassName() != null) {
                Long cid = resolveClassId(dto.getClassName(), st.getCounselorId());
                if (cid != null) st.setClassId(cid);
            }
            studentMapper.updateById(st);
        } else if ("TEACHER".equals(role) && te != null) {
            if (StringUtils.hasText(dto.getRealName())) te.setRealName(dto.getRealName());
            if (dto.getStudentNo() != null) te.setTeacherNo(dto.getStudentNo());
            if (dto.getPhone() != null) te.setPhone(dto.getPhone());
            teacherMapper.updateById(te);
        }

        return Result.ok(userMapper.enrichById(id));
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

    /** 辅导员下拉：id 用辅导员的 sys_user.id（便于“新增学生指定辅导员”换算 counselor_id） */
    @GetMapping("/teachers")
    public Result<List<Map<String, Object>>> teachers() {
        return Result.ok(userMapper.teacherOptions());
    }

    // ---------------- 内部辅助 ----------------

    private Long roleIdOf(String roleCode) {
        SysRole r = roleMapper.selectOne(new QueryWrapper<SysRole>().eq("role_code", roleCode));
        if (r == null) throw BizException.badParam("角色不存在: " + roleCode);
        return r.getId();
    }

    private Teacher teacherByUserId(Long userId) {
        return teacherMapper.selectOne(new QueryWrapper<Teacher>().eq("user_id", userId));
    }

    private Student studentByUserId(Long userId) {
        return studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", userId));
    }

    /**
     * 由班级名解析 class_info.id；找不到则回退辅导员名下已有学生的班级，保证 className 能落库展示。
     * className 为空返回 null（不改班级）。
     */
    private Long resolveClassId(String className, Long counselorTeacherId) {
        if (StringUtils.hasText(className)) {
            ClassInfo c = classInfoMapper.selectOne(new QueryWrapper<ClassInfo>()
                    .eq("class_name", className).last("LIMIT 1"));
            if (c != null) return c.getId();
        }
        if (counselorTeacherId != null) {
            Student any = studentMapper.selectOne(new QueryWrapper<Student>()
                    .eq("counselor_id", counselorTeacherId).isNotNull("class_id").last("LIMIT 1"));
            if (any != null) return any.getClassId();
        }
        return null;
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
