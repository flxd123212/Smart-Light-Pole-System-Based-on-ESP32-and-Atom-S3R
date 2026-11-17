package com.iot.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.iotlight.common.annotation.Log;
import com.iotlight.common.core.controller.BaseController;
import com.iotlight.common.core.domain.AjaxResult;
import com.iotlight.common.enums.BusinessType;
import com.iot.domain.IotAlarmRule;
import com.iot.service.IIotAlarmRuleService;
import com.iotlight.common.utils.poi.ExcelUtil;
import com.iotlight.common.core.page.TableDataInfo;

/**
 * 报警规则Controller
 * 
 * @author extrao
 * @date 2026-07-24
 */
@RestController
@RequestMapping("/IotAlarmRule/IotAlarmRule")
public class IotAlarmRuleController extends BaseController
{
    @Autowired
    private IIotAlarmRuleService iotAlarmRuleService;

    /**
     * 查询报警规则列表
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRule:IotAlarmRule:list')")
    @GetMapping("/list")
    public TableDataInfo list(IotAlarmRule iotAlarmRule)
    {
        startPage();
        List<IotAlarmRule> list = iotAlarmRuleService.selectIotAlarmRuleList(iotAlarmRule);
        return getDataTable(list);
    }

    /**
     * 导出报警规则列表
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRule:IotAlarmRule:export')")
    @Log(title = "报警规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, IotAlarmRule iotAlarmRule)
    {
        List<IotAlarmRule> list = iotAlarmRuleService.selectIotAlarmRuleList(iotAlarmRule);
        ExcelUtil<IotAlarmRule> util = new ExcelUtil<IotAlarmRule>(IotAlarmRule.class);
        util.exportExcel(response, list, "报警规则数据");
    }

    /**
     * 获取报警规则详细信息
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRule:IotAlarmRule:query')")
    @GetMapping(value = "/{ruleId}")
    public AjaxResult getInfo(@PathVariable("ruleId") Long ruleId)
    {
        return success(iotAlarmRuleService.selectIotAlarmRuleByRuleId(ruleId));
    }

    /**
     * 新增报警规则
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRule:IotAlarmRule:add')")
    @Log(title = "报警规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody IotAlarmRule iotAlarmRule)
    {
        return toAjax(iotAlarmRuleService.insertIotAlarmRule(iotAlarmRule));
    }

    /**
     * 修改报警规则
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRule:IotAlarmRule:edit')")
    @Log(title = "报警规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody IotAlarmRule iotAlarmRule)
    {
        return toAjax(iotAlarmRuleService.updateIotAlarmRule(iotAlarmRule));
    }

    /**
     * 删除报警规则
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRule:IotAlarmRule:remove')")
    @Log(title = "报警规则", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ruleIds}")
    public AjaxResult remove(@PathVariable Long[] ruleIds)
    {
        return toAjax(iotAlarmRuleService.deleteIotAlarmRuleByRuleIds(ruleIds));
    }
}
