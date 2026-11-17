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
import com.iot.domain.IotCameraCapture;
import com.iot.service.IIotCameraCaptureService;
import com.iotlight.common.utils.poi.ExcelUtil;
import com.iotlight.common.core.page.TableDataInfo;

/**
 * 摄像头抓拍记录Controller
 * 
 * @author extrao
 * @date 2026-07-24
 */
@RestController
@RequestMapping("/IotCameraCapture/IotCameraCapture")
public class IotCameraCaptureController extends BaseController
{
    @Autowired
    private IIotCameraCaptureService iotCameraCaptureService;

    /**
     * 查询摄像头抓拍记录列表
     */
    @PreAuthorize("@ss.hasPermi('IotCameraCapture:IotCameraCapture:list')")
    @GetMapping("/list")
    public TableDataInfo list(IotCameraCapture iotCameraCapture)
    {
        startPage();
        List<IotCameraCapture> list = iotCameraCaptureService.selectIotCameraCaptureList(iotCameraCapture);
        return getDataTable(list);
    }

    /**
     * 导出摄像头抓拍记录列表
     */
    @PreAuthorize("@ss.hasPermi('IotCameraCapture:IotCameraCapture:export')")
    @Log(title = "摄像头抓拍记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, IotCameraCapture iotCameraCapture)
    {
        List<IotCameraCapture> list = iotCameraCaptureService.selectIotCameraCaptureList(iotCameraCapture);
        ExcelUtil<IotCameraCapture> util = new ExcelUtil<IotCameraCapture>(IotCameraCapture.class);
        util.exportExcel(response, list, "摄像头抓拍记录数据");
    }

    /**
     * 获取摄像头抓拍记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('IotCameraCapture:IotCameraCapture:query')")
    @GetMapping(value = "/{captureId}")
    public AjaxResult getInfo(@PathVariable("captureId") Long captureId)
    {
        return success(iotCameraCaptureService.selectIotCameraCaptureByCaptureId(captureId));
    }

    /**
     * 新增摄像头抓拍记录
     */
    @PreAuthorize("@ss.hasPermi('IotCameraCapture:IotCameraCapture:add')")
    @Log(title = "摄像头抓拍记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody IotCameraCapture iotCameraCapture)
    {
        return toAjax(iotCameraCaptureService.insertIotCameraCapture(iotCameraCapture));
    }

    /**
     * 修改摄像头抓拍记录
     */
    @PreAuthorize("@ss.hasPermi('IotCameraCapture:IotCameraCapture:edit')")
    @Log(title = "摄像头抓拍记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody IotCameraCapture iotCameraCapture)
    {
        return toAjax(iotCameraCaptureService.updateIotCameraCapture(iotCameraCapture));
    }

    /**
     * 删除摄像头抓拍记录
     */
    @PreAuthorize("@ss.hasPermi('IotCameraCapture:IotCameraCapture:remove')")
    @Log(title = "摄像头抓拍记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{captureIds}")
    public AjaxResult remove(@PathVariable Long[] captureIds)
    {
        return toAjax(iotCameraCaptureService.deleteIotCameraCaptureByCaptureIds(captureIds));
    }
}
