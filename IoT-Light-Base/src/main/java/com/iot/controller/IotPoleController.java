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
import com.iot.service.IIotMqttPublishService;
import com.iotlight.common.utils.poi.ExcelUtil;
import com.iotlight.common.core.page.TableDataInfo;
import com.iot.mapper.IotPoleMapper;
import com.iot.mapper.IotDeviceMapper;
import com.iot.mapper.IotSensorDataMapper;

import java.util.Map;

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

    @Autowired
    private IIotMqttPublishService iotMqttPublishService;

    @Autowired
    private IotPoleMapper iotPoleMapper;

    @Autowired
    private IotDeviceMapper iotDeviceMapper;

    @Autowired
    private IotSensorDataMapper iotSensorDataMapper;

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
     * 停用灯杆（软删除：状态→停用，del_flag=2）
     * 级联停用关联设备 + 清除传感器数据 + MQTT通知Flask
     */
    @PreAuthorize("@ss.hasPermi('IotPole:IotPole:remove')")
    @Log(title = "灯杆", businessType = BusinessType.UPDATE)
	@DeleteMapping("/{poleIds}")
    public AjaxResult remove(@PathVariable Long[] poleIds)
    {
        for (Long poleId : poleIds)
        {
            try
            {
                // 1. 级联软删除关联设备
                iotDeviceMapper.deleteIotDeviceByPoleId(poleId);
                // 2. 清除传感器数据
                iotSensorDataMapper.deleteIotSensorDataByPoleId(poleId);
                // 3. MQTT通知Flask停止上报
                IotPole pole = iotPoleMapper.selectIotPoleByPoleId(poleId);
                if (pole != null && pole.getPoleCode() != null)
                {
                    iotMqttPublishService.sendPoleCommand(pole.getPoleCode(), "disable", null);
                }
            }
            catch (Exception e)
            {
                // 级联操作失败不影响灯杆停用
            }
        }
        return toAjax(iotPoleService.deleteIotPoleByPoleIds(poleIds));
    }

    /**
     * 灯杆 LED 控制 (通过 MQTT 下发到 ESP32)
     * POST /IotPole/IotPole/control
     * body: {"poleCode":"LP001","command":"on","value":80}
     * command: on/off/dim
     */
    @PreAuthorize("@ss.hasPermi('IotPole:IotPole:edit')")
    @Log(title = "灯杆控制", businessType = BusinessType.UPDATE)
    @PostMapping("/control")
    public AjaxResult control(@RequestBody Map<String, Object> body)
    {
        String poleCode = body.get("poleCode") != null ? body.get("poleCode").toString() : null;
        String command = body.get("command") != null ? body.get("command").toString() : null;
        Integer value = null;
        if (body.get("value") != null)
        {
            try
            {
                value = Integer.valueOf(body.get("value").toString());
            }
            catch (NumberFormatException ignored)
            {
            }
        }
        if (poleCode == null || poleCode.isEmpty() || command == null || command.isEmpty())
        {
            return error("poleCode 和 command 不能为空");
        }
        boolean ok = iotMqttPublishService.sendPoleCommand(poleCode, command, value);
        return ok ? success() : error("MQTT 下发失败, 请检查 EMQX 与设备连接");
    }

    /**
     * 触发摄像头抓拍 (通过 MQTT 通知 AI 侧)
     * POST /IotPole/IotPole/camera/trigger
     * body: {"cameraCode":"CAM001"}
     */
    @PreAuthorize("@ss.hasPermi('IotPole:IotPole:edit')")
    @Log(title = "摄像头抓拍", businessType = BusinessType.UPDATE)
    @PostMapping("/camera/trigger")
    public AjaxResult cameraTrigger(@RequestBody Map<String, Object> body)
    {
        String cameraCode = body.get("cameraCode") != null ? body.get("cameraCode").toString() : "CAM001";
        boolean ok = iotMqttPublishService.triggerCameraCapture(cameraCode);
        return ok ? success() : error("MQTT 下发失败, 请检查 EMQX 连接");
    }
}
