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
import com.iot.domain.IotSensorData;
import com.iot.service.IIotSensorDataService;
import com.iotlight.common.utils.poi.ExcelUtil;
import com.iotlight.common.core.page.TableDataInfo;

/**
 * 传感器数据Controller
 * 
 * @author extrao
 * @date 2026-07-24
 */
@RestController
@RequestMapping("/IotSensorData/IotSensorData")
public class IotSensorDataController extends BaseController
{
    @Autowired
    private IIotSensorDataService iotSensorDataService;

    /**
     * 查询传感器数据列表
     */
    @PreAuthorize("@ss.hasPermi('IotSensorData:IotSensorData:list')")
    @GetMapping("/list")
    public TableDataInfo list(IotSensorData iotSensorData)
    {
        startPage();
        List<IotSensorData> list = iotSensorDataService.selectIotSensorDataList(iotSensorData);
        return getDataTable(list);
    }

    /**
     * 导出传感器数据列表
     */
    @PreAuthorize("@ss.hasPermi('IotSensorData:IotSensorData:export')")
    @Log(title = "传感器数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, IotSensorData iotSensorData)
    {
        List<IotSensorData> list = iotSensorDataService.selectIotSensorDataList(iotSensorData);
        ExcelUtil<IotSensorData> util = new ExcelUtil<IotSensorData>(IotSensorData.class);
        util.exportExcel(response, list, "传感器数据数据");
    }

    /**
     * 获取传感器数据详细信息
     */
    @PreAuthorize("@ss.hasPermi('IotSensorData:IotSensorData:query')")
    @GetMapping(value = "/{dataId}")
    public AjaxResult getInfo(@PathVariable("dataId") Long dataId)
    {
        return success(iotSensorDataService.selectIotSensorDataByDataId(dataId));
    }

    /**
     * 新增传感器数据
     */
    @PreAuthorize("@ss.hasPermi('IotSensorData:IotSensorData:add')")
    @Log(title = "传感器数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody IotSensorData iotSensorData)
    {
        return toAjax(iotSensorDataService.insertIotSensorData(iotSensorData));
    }

    /**
     * 修改传感器数据
     */
    @PreAuthorize("@ss.hasPermi('IotSensorData:IotSensorData:edit')")
    @Log(title = "传感器数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody IotSensorData iotSensorData)
    {
        return toAjax(iotSensorDataService.updateIotSensorData(iotSensorData));
    }

    /**
     * 删除传感器数据
     */
    @PreAuthorize("@ss.hasPermi('IotSensorData:IotSensorData:remove')")
    @Log(title = "传感器数据", businessType = BusinessType.DELETE)
	@DeleteMapping("/{dataIds}")
    public AjaxResult remove(@PathVariable Long[] dataIds)
    {
        return toAjax(iotSensorDataService.deleteIotSensorDataByDataIds(dataIds));
    }
}
