package com.school.leave.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/** 分页统一返回 { records, total, current, size } */
@Data
public class PageVO<T> {
    private List<T> records;
    private long total;
    private long current;
    private long size;

    public static <T> PageVO<T> of(IPage<T> page) {
        PageVO<T> vo = new PageVO<>();
        vo.records = page.getRecords();
        vo.total = page.getTotal();
        vo.current = page.getCurrent();
        vo.size = page.getSize();
        return vo;
    }
}
