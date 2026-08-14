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
import com.iot.domain.IotAlarmRecord;
import com.iot.service.IIotAlarmRecordService;
import com.iotlight.common.utils.poi.ExcelUtil;
import com.iotlight.common.core.page.TableDataInfo;

/**
 * 报警记录Controller
 * 
 * @author extrao
 * @date 2026-07-24
 */
@RestController
@RequestMapping("/IotAlarmRecord/IotAlarmRecord")
public class IotAlarmRecordController extends BaseController
{
    @Autowired
    private IIotAlarmRecordService iotAlarmRecordService;

    /**
     * 查询报警记录列表
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRecord:IotAlarmRecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(IotAlarmRecord iotAlarmRecord)
    {
        startPage();
        List<IotAlarmRecord> list = iotAlarmRecordService.selectIotAlarmRecordList(iotAlarmRecord);
        return getDataTable(list);
    }

    /**
     * 导出报警记录列表
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRecord:IotAlarmRecord:export')")
    @Log(title = "报警记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, IotAlarmRecord iotAlarmRecord)
    {
        List<IotAlarmRecord> list = iotAlarmRecordService.selectIotAlarmRecordList(iotAlarmRecord);
        ExcelUtil<IotAlarmRecord> util = new ExcelUtil<IotAlarmRecord>(IotAlarmRecord.class);
        util.exportExcel(response, list, "报警记录数据");
    }

    /**
     * 获取报警记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRecord:IotAlarmRecord:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(iotAlarmRecordService.selectIotAlarmRecordByRecordId(recordId));
    }

    /**
     * 新增报警记录
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRecord:IotAlarmRecord:add')")
    @Log(title = "报警记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody IotAlarmRecord iotAlarmRecord)
    {
        return toAjax(iotAlarmRecordService.insertIotAlarmRecord(iotAlarmRecord));
    }

    /**
     * 修改报警记录
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRecord:IotAlarmRecord:edit')")
    @Log(title = "报警记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody IotAlarmRecord iotAlarmRecord)
    {
        return toAjax(iotAlarmRecordService.updateIotAlarmRecord(iotAlarmRecord));
    }

    /**
     * 删除报警记录
     */
    @PreAuthorize("@ss.hasPermi('IotAlarmRecord:IotAlarmRecord:remove')")
    @Log(title = "报警记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(iotAlarmRecordService.deleteIotAlarmRecordByRecordIds(recordIds));
    }
}
