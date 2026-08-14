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
import com.iot.domain.IotPole;
import com.iot.service.IIotPoleService;
import com.iotlight.common.utils.poi.ExcelUtil;
import com.iotlight.common.core.page.TableDataInfo;

/**
 * 灯杆Controller
 * 
 * @author extrao
 * @date 2026-07-24
 */
@RestController
@RequestMapping("/IotPole/IotPole")
public class IotPoleController extends BaseController
{
    @Autowired
    private IIotPoleService iotPoleService;

    /**
     * 查询灯杆列表
     */
    @PreAuthorize("@ss.hasPermi('IotPole:IotPole:list')")
    @GetMapping("/list")
    public TableDataInfo list(IotPole iotPole)
    {
        startPage();
        List<IotPole> list = iotPoleService.selectIotPoleList(iotPole);
        return getDataTable(list);
    }

    /**
     * 导出灯杆列表
     */
    @PreAuthorize("@ss.hasPermi('IotPole:IotPole:export')")
    @Log(title = "灯杆", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, IotPole iotPole)
    {
        List<IotPole> list = iotPoleService.selectIotPoleList(iotPole);
        ExcelUtil<IotPole> util = new ExcelUtil<IotPole>(IotPole.class);
        util.exportExcel(response, list, "灯杆数据");
    }

    /**
     * 获取灯杆详细信息
     */
    @PreAuthorize("@ss.hasPermi('IotPole:IotPole:query')")
    @GetMapping(value = "/{poleId}")
    public AjaxResult getInfo(@PathVariable("poleId") Long poleId)
    {
        return success(iotPoleService.selectIotPoleByPoleId(poleId));
    }

    /**
     * 新增灯杆
     */
    @PreAuthorize("@ss.hasPermi('IotPole:IotPole:add')")
    @Log(title = "灯杆", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody IotPole iotPole)
    {
        return toAjax(iotPoleService.insertIotPole(iotPole));
    }

    /**
     * 修改灯杆
     */
    @PreAuthorize("@ss.hasPermi('IotPole:IotPole:edit')")
    @Log(title = "灯杆", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody IotPole iotPole)
    {
        return toAjax(iotPoleService.updateIotPole(iotPole));
    }

    /**
     * 删除灯杆
     */
    @PreAuthorize("@ss.hasPermi('IotPole:IotPole:remove')")
    @Log(title = "灯杆", businessType = BusinessType.DELETE)
	@DeleteMapping("/{poleIds}")
    public AjaxResult remove(@PathVariable Long[] poleIds)
    {
        return toAjax(iotPoleService.deleteIotPoleByPoleIds(poleIds));
    }
}
