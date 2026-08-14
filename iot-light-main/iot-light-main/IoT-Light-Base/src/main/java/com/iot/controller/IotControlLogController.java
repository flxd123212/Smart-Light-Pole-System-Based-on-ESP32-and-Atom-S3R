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
import com.iot.domain.IotControlLog;
import com.iot.service.IIotControlLogService;
import com.iotlight.common.utils.poi.ExcelUtil;
import com.iotlight.common.core.page.TableDataInfo;

/**
 * 控制日志Controller
 * 
 * @author extrao
 * @date 2026-07-24
 */
@RestController
@RequestMapping("/IotControlLog/IotControlLog")
public class IotControlLogController extends BaseController
{
    @Autowired
    private IIotControlLogService iotControlLogService;

    /**
     * 查询控制日志列表
     */
    @PreAuthorize("@ss.hasPermi('IotControlLog:IotControlLog:list')")
    @GetMapping("/list")
    public TableDataInfo list(IotControlLog iotControlLog)
    {
        startPage();
        List<IotControlLog> list = iotControlLogService.selectIotControlLogList(iotControlLog);
        return getDataTable(list);
    }

    /**
     * 导出控制日志列表
     */
    @PreAuthorize("@ss.hasPermi('IotControlLog:IotControlLog:export')")
    @Log(title = "控制日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, IotControlLog iotControlLog)
    {
        List<IotControlLog> list = iotControlLogService.selectIotControlLogList(iotControlLog);
        ExcelUtil<IotControlLog> util = new ExcelUtil<IotControlLog>(IotControlLog.class);
        util.exportExcel(response, list, "控制日志数据");
    }

    /**
     * 获取控制日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('IotControlLog:IotControlLog:query')")
    @GetMapping(value = "/{logId}")
    public AjaxResult getInfo(@PathVariable("logId") Long logId)
    {
        return success(iotControlLogService.selectIotControlLogByLogId(logId));
    }

    /**
     * 新增控制日志
     */
    @PreAuthorize("@ss.hasPermi('IotControlLog:IotControlLog:add')")
    @Log(title = "控制日志", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody IotControlLog iotControlLog)
    {
        return toAjax(iotControlLogService.insertIotControlLog(iotControlLog));
    }

    /**
     * 修改控制日志
     */
    @PreAuthorize("@ss.hasPermi('IotControlLog:IotControlLog:edit')")
    @Log(title = "控制日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody IotControlLog iotControlLog)
    {
        return toAjax(iotControlLogService.updateIotControlLog(iotControlLog));
    }

    /**
     * 删除控制日志
     */
    @PreAuthorize("@ss.hasPermi('IotControlLog:IotControlLog:remove')")
    @Log(title = "控制日志", businessType = BusinessType.DELETE)
	@DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(iotControlLogService.deleteIotControlLogByLogIds(logIds));
    }
}
