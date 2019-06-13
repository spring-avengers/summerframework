package com.bkjk.platform.dts.ops.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bkjk.platform.dts.ops.dao.BranchErrorRecordMapper;
import com.bkjk.platform.dts.ops.dao.BranchRecordMapper;
import com.bkjk.platform.dts.ops.dao.GlobalRecordMapper;
import com.bkjk.platform.dts.ops.domain.BranchErrorRecord;
import com.bkjk.platform.dts.ops.domain.BranchRecord;
import com.bkjk.platform.dts.ops.domain.GlobalRecord;
import com.bkjk.platform.dts.ops.vo.BranchErrorRecordVo;
import com.bkjk.platform.dts.ops.vo.BranchLogState;
import com.bkjk.platform.dts.ops.vo.BranchRecordVo;
import com.bkjk.platform.dts.ops.vo.GlobalRecordVo;
import com.bkjk.platform.dts.ops.vo.PageVO;

@Controller
@RequestMapping(value = "/branch")
public class BranchRecordController {
    @Autowired
    private BranchRecordMapper branchRecordMapper;

    @Autowired
    private BranchErrorRecordMapper branchErrorRecordMapper;
    @Autowired
    private GlobalRecordMapper globalRecordMapper;

    @RequestMapping("/error/{branchId}")
    @ResponseBody
    public BranchErrorRecordVo branchError(@PathVariable("branchId") Long branchId, Model model) {
        BranchErrorRecord errorRecord =
            branchErrorRecordMapper.selectOne(Wrappers.<BranchErrorRecord>query().eq("branch_id", branchId));
        BranchErrorRecordVo vo = new BranchErrorRecordVo();
        BeanUtils.copyProperties(errorRecord, vo);
        return vo;
    }

    @RequestMapping("/list/{transId}")
    public String branchList(@PathVariable("transId") Long transId, Model model) {
        model.addAttribute("BranchLogStates", BranchLogState.values());
        GlobalRecord globalRecord =
            globalRecordMapper.selectOne(Wrappers.<GlobalRecord>query().eq("trans_Id", transId));
        GlobalRecordVo vo = new GlobalRecordVo();
        BeanUtils.copyProperties(globalRecord, vo);
        model.addAttribute("globalRecord", vo);
        return "branch/list";
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public PageVO<BranchRecordVo> getBranchRecords(@RequestParam Map<String, Object> params) {
        int offset = Integer.parseInt(params.get("offset").toString());
        int pageSize = Integer.parseInt(params.get("limit").toString());
        Long transId = Long.parseLong(params.get("transId").toString());
        int state = Integer.parseInt(params.get("state").toString());
        int current = offset / pageSize + 1;
        Page pageRequest = new Page(current, pageSize);
        QueryWrapper<BranchRecord> query = Wrappers.<BranchRecord>query();
        query.eq("trans_id", transId);
        if (state > 0) {
            query.eq("state", state);
        }
        query.orderByDesc("gmt_created", "gmt_modified");
        IPage<BranchRecord> records = branchRecordMapper.selectPage(pageRequest, query);
        List<BranchRecordVo> vos = records.getRecords().stream().map(domain -> {
            BranchRecordVo vo = new BranchRecordVo();
            BeanUtils.copyProperties(domain, vo);
            return vo;
        }).collect(Collectors.toList());
        PageVO vo = new PageVO(vos, records.getTotal(), current, pageSize);
        return vo;
    }
}
