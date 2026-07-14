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

    String SELECT_VO = "SELECT lr.id, lr.student_id, lr.type, lr.start_time, lr.end_time, lr.days, lr.reason, " +
            "lr.destination, lr.contact_phone, lr.status, lr.approve_comment, lr.approve_time, " +
            "lr.cancel_apply_time, lr.cancel_note, lr.complete_time, lr.create_time, " +
            "s.real_name AS student_name, s.student_no, s.class_name, a.real_name AS approver_name " +
            "FROM leave_request lr " +
            "JOIN sys_user s ON lr.student_id = s.id " +
            "LEFT JOIN sys_user a ON lr.approver_id = a.id";

    @Select("<script>" + SELECT_VO +
            " WHERE lr.student_id = #{sid}" +
            "<if test='status != null and status != \"\"'> AND lr.status = #{status}</if>" +
            " ORDER BY lr.create_time DESC</script>")
    IPage<LeaveVO> pageMy(IPage<LeaveVO> page, @Param("sid") Long sid, @Param("status") String status);

    @Select(SELECT_VO + " WHERE lr.id = #{id}")
    LeaveVO findVoById(@Param("id") Long id);

    @Select(SELECT_VO + " WHERE s.teacher_id = #{tid} AND lr.status IN ('PENDING','CANCEL_PENDING')" +
            " ORDER BY FIELD(lr.status,'PENDING','CANCEL_PENDING'), lr.create_time ASC")
    IPage<LeaveVO> pagePending(IPage<LeaveVO> page, @Param("tid") Long tid);

    @Select(SELECT_VO + " WHERE lr.approver_id = #{tid} ORDER BY lr.approve_time DESC")
    IPage<LeaveVO> pageHistory(IPage<LeaveVO> page, @Param("tid") Long tid);

    /** 副书记待办：所有 LEADER_PENDING */
    @Select(SELECT_VO + " WHERE lr.status = 'LEADER_PENDING' ORDER BY lr.create_time ASC")
    IPage<LeaveVO> pageLeaderPending(IPage<LeaveVO> page);

    // ------- 请假次数排名 -------
    /** 名下学生请假次数排名（tid 非空=仅该辅导员名下；为空=全部）；排除 REVOKED */
    @Select("<script>SELECT s.id AS studentId, s.real_name AS studentName, s.student_no AS studentNo, " +
            "s.class_name AS className, COUNT(lr.id) AS leaveCount " +
            "FROM sys_user s LEFT JOIN leave_request lr ON lr.student_id = s.id AND lr.status &lt;&gt; 'REVOKED' " +
            "WHERE s.role = 'STUDENT'" +
            "<if test='tid != null'> AND s.teacher_id = #{tid}</if>" +
            " GROUP BY s.id, s.real_name, s.student_no, s.class_name " +
            "ORDER BY leaveCount DESC, s.student_no ASC</script>")
    List<Map<String, Object>> ranking(@Param("tid") Long tid);

    // ------- 导出（不分页） -------
    @Select("<script>" + SELECT_VO +
            "<choose>" +
            "<when test='tid != null'> WHERE s.teacher_id = #{tid}</when>" +
            "<otherwise> WHERE 1=1</otherwise>" +
            "</choose>" +
            "<if test='status != null and status != \"\"'> AND lr.status = #{status}</if>" +
            " ORDER BY lr.create_time DESC</script>")
    List<LeaveVO> exportList(@Param("tid") Long tid, @Param("status") String status);

    @Select("<script>" + SELECT_VO + " WHERE 1=1" +
            "<if test='status != null and status != \"\"'> AND lr.status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (s.real_name LIKE CONCAT('%',#{keyword},'%')" +
            " OR s.username LIKE CONCAT('%',#{keyword},'%') OR s.student_no LIKE CONCAT('%',#{keyword},'%'))</if>" +
            " ORDER BY lr.create_time DESC</script>")
    IPage<LeaveVO> pageAdmin(IPage<LeaveVO> page, @Param("status") String status, @Param("keyword") String keyword);

    @Select("SELECT ar.action, ar.comment, ar.create_time, u.real_name AS operator_name " +
            "FROM approval_record ar JOIN sys_user u ON ar.operator_id = u.id " +
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
