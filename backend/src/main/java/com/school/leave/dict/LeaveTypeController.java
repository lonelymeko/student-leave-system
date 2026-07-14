package com.school.leave.dict;

import com.school.leave.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 请假类型字典接口（登录即可） */
@RestController
@RequestMapping("/leave-types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @GetMapping
    public Result<List<LeaveTypeEntity>> list() {
        return Result.ok(leaveTypeService.listEnabled());
    }
}
