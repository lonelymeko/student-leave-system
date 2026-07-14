package com.school.leave.leave;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface LeaveMapper extends BaseMapper<LeaveRequest> {

    // 规范化后：学生姓名/学号/班级取自 student(+class_info)（经 lr.student_id → student.user_id）；
    // 审批人姓名取自 teacher（经 lr.approver_id → teacher.user_id，审批人均为教师）。
    String SELECT_VO = "SELECT lr.id, lr.student_id, lr.type, lr.start_time, lr.end_time, lr.days, lr.reason, " +
            "lr.destination, lr.contact_phone, lr.status, lr.approve_comment, lr.approve_time, " +
            "lr.cancel_apply_time, lr.cancel_note, lr.complete_time, lr.create_time, " +
            "s.real_name AS student_name, s.student_no, ci.class_name AS class_name, at.real_name AS approver_name " +
            "FROM leave_request lr " +
            "JOIN student s ON s.user_id = lr.student_id " +
            "LEFT JOIN class_info ci ON ci.id = s.class_id " +
            "LEFT JOIN teacher at ON at.user_id = lr.approver_id";

    @Select("<script>" + SELECT_VO +
            " WHERE lr.student_id = #{sid}" +
            "<if test='status != null and status != \"\"'> AND lr.status = #{status}</if>" +
            " ORDER BY lr.create_time DESC</script>")
    IPage<LeaveVO> pageMy(IPage<LeaveVO> page, @Param("sid") Long sid, @Param("status") String status);

    @Select(SELECT_VO + " WHERE lr.id = #{id}")
    LeaveVO findVoById(@Param("id") Long id);

    // 辅导员待办：名下学生 = student.counselor_id 对应 teacher.user_id = 当前教师账号 uid(tid)
    @Select(SELECT_VO +
            " JOIN teacher ct ON ct.id = s.counselor_id" +
            " WHERE ct.user_id = #{tid} AND lr.status IN ('PENDING','CANCEL_PENDING')" +
            " ORDER BY FIELD(lr.status,'PENDING','CANCEL_PENDING'), lr.create_time ASC")
    IPage<LeaveVO> pagePending(IPage<LeaveVO> page, @Param("tid") Long tid);

    @Select(SELECT_VO + " WHERE lr.approver_id = #{tid} ORDER BY lr.approve_time DESC")
    IPage<LeaveVO> pageHistory(IPage<LeaveVO> page, @Param("tid") Long tid);

    /** 副书记待办：所有 LEADER_PENDING */
    @Select(SELECT_VO + " WHERE lr.status = 'LEADER_PENDING' ORDER BY lr.create_time ASC")
    IPage<LeaveVO> pageLeaderPending(IPage<LeaveVO> page);

    // ------- 请假次数排名 -------
    /** 名下学生请假次数排名（tid 非空=仅该辅导员名下 uid；为空=全部）；排除 REVOKED。
     *  按 student 分组，输出 studentName/studentNo/className/leaveCount。 */
    @Select("<script>SELECT s.user_id AS studentId, s.real_name AS studentName, s.student_no AS studentNo, " +
            "ci.class_name AS className, COUNT(lr.id) AS leaveCount " +
            "FROM student s " +
            "LEFT JOIN class_info ci ON ci.id = s.class_id " +
            "LEFT JOIN teacher ct ON ct.id = s.counselor_id " +
            "LEFT JOIN leave_request lr ON lr.student_id = s.user_id AND lr.status &lt;&gt; 'REVOKED' " +
            "<where>" +
            "<if test='tid != null'> ct.user_id = #{tid}</if>" +
            "</where>" +
            " GROUP BY s.user_id, s.real_name, s.student_no, ci.class_name " +
            "ORDER BY leaveCount DESC, s.student_no ASC</script>")
    List<Map<String, Object>> ranking(@Param("tid") Long tid);

    // ------- 导出（不分页） -------
    @Select("<script>" + SELECT_VO +
            "<choose>" +
            "<when test='tid != null'> JOIN teacher ct ON ct.id = s.counselor_id WHERE ct.user_id = #{tid}</when>" +
            "<otherwise> WHERE 1=1</otherwise>" +
            "</choose>" +
            "<if test='status != null and status != \"\"'> AND lr.status = #{status}</if>" +
            " ORDER BY lr.create_time DESC</script>")
    List<LeaveVO> exportList(@Param("tid") Long tid, @Param("status") String status);

    @Select("<script>" + SELECT_VO +
            " JOIN sys_user su ON su.id = lr.student_id WHERE 1=1" +
            "<if test='status != null and status != \"\"'> AND lr.status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (s.real_name LIKE CONCAT('%',#{keyword},'%')" +
            " OR su.username LIKE CONCAT('%',#{keyword},'%') OR s.student_no LIKE CONCAT('%',#{keyword},'%'))</if>" +
            " ORDER BY lr.create_time DESC</script>")
    IPage<LeaveVO> pageAdmin(IPage<LeaveVO> page, @Param("status") String status, @Param("keyword") String keyword);

    // 操作人姓名：operator 均为学生或教师，两表 COALESCE 取姓名
    @Select("SELECT ar.action, ar.comment, ar.create_time, " +
            "COALESCE(stu.real_name, tea.real_name) AS operator_name " +
            "FROM approval_record ar " +
            "LEFT JOIN student stu ON stu.user_id = ar.operator_id " +
            "LEFT JOIN teacher tea ON tea.user_id = ar.operator_id " +
            "WHERE ar.leave_id = #{leaveId} ORDER BY ar.create_time ASC, ar.id ASC")
    List<RecordVO> findRecords(@Param("leaveId") Long leaveId);

    // ------- 统计 -------
    @Select("SELECT type, COUNT(*) AS cnt FROM leave_request GROUP BY type")
    List<Map<String, Object>> countByType();

    @Select("SELECT status, COUNT(*) AS cnt FROM leave_request GROUP BY status")
    List<Map<String, Object>> countByStatus();

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS cnt FROM leave_request " +
            "WHERE create_time >= #{since} GROUP BY DATE_FORMAT(create_time, '%Y-%m')")
    List<Map<String, Object>> countByMonth(@Param("since") LocalDateTime since);

    // ------- AI 审批建议上下文：该生近半年请假统计 -------
    @Select("SELECT COUNT(*) FROM leave_request WHERE student_id = #{sid} AND create_time >= #{since}")
    long countRecentByStudent(@Param("sid") Long sid, @Param("since") LocalDateTime since);

    @Select("SELECT IFNULL(SUM(days),0) FROM leave_request WHERE student_id = #{sid} AND create_time >= #{since} " +
            "AND status IN ('APPROVED','CANCEL_PENDING','COMPLETED')")
    BigDecimal sumRecentDaysByStudent(@Param("sid") Long sid, @Param("since") LocalDateTime since);

    @Select("SELECT COUNT(*) FROM leave_request WHERE student_id = #{sid} AND create_time >= #{since} AND status = 'REJECTED'")
    long countRecentRejectedByStudent(@Param("sid") Long sid, @Param("since") LocalDateTime since);
}
