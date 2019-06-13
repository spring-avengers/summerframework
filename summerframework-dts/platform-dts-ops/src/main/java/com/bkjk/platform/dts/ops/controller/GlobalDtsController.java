package com.bkjk.platform.dts.ops.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bkjk.platform.dts.ops.dao.GlobalRecordMapper;
import com.bkjk.platform.dts.ops.domain.GlobalRecord;
import com.bkjk.platform.dts.ops.vo.GlobalRecordVo;
import com.bkjk.platform.dts.ops.vo.PageVO;

@RestController
@RequestMapping(value = "/globalRecord")
public class GlobalDtsController {

    @Autowired
    private GlobalRecordMapper globalRecordMapper;

    private Date formatDate(String param) {
        if (StringUtils.isEmpty(param)) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-mm-dd HH:mm").parse(param);
        } catch (ParseException e) {
            return null;
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public PageVO<GlobalRecordVo> getGlobalRecord(@RequestParam Map<String, Object> params) {
        int offset = Integer.parseInt(params.get("offset").toString());
        int pageSize = Integer.parseInt(params.get("limit").toString());
        int state = Integer.parseInt(params.get("state").toString());
        Date startTime = formatDate(params.get("startTime").toString());
        Date endTime = formatDate(params.get("endTime").toString());
        int current = offset / pageSize + 1;
        Page pageRequest = new Page(current, pageSize);
        QueryWrapper<GlobalRecord> query = Wrappers.<GlobalRecord>query();
        if (state > 0) {
            query.eq("state", state);
        }
        query.orderByDesc("gmt_created", "gmt_modified");
        if (!Objects.isNull(startTime)) {
            if (!Objects.isNull(endTime)) {
                query.between("gmt_created", startTime, endTime);
            } else {
                query.ge("gmt_created", startTime);
            }
        } else if (!Objects.isNull(endTime)) {
            query.le("gmt_created", endTime);
        }
        IPage<GlobalRecord> records = globalRecordMapper.selectPage(pageRequest, query);
        List<GlobalRecordVo> vos = records.getRecords().stream().map(domain -> {
            GlobalRecordVo vo = new GlobalRecordVo();
            BeanUtils.copyProperties(domain, vo);
            return vo;
        }).collect(Collectors.toList());
        PageVO vo = new PageVO(vos, records.getTotal(), current, pageSize);
        return vo;
    }
}
