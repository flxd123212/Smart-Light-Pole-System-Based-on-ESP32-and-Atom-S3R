package com.iotlight.web.controller.iotlight;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.iotlight.common.core.controller.BaseController;
import com.iotlight.common.core.domain.AjaxResult;
import com.iot.service.IIotMqttPublishService;
import com.iot.service.IIotPoleService;

import java.util.HashMap;
import java.util.Map;

/**
 * MQTT 控制器 - 提供设备控制 HTTP API
 *
 * @author iot
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/mqtt")
public class IotMqttController extends BaseController
{
  @Autowired
  private IIotMqttPublishService mqttPublishService;

  @Autowired
  private IIotPoleService iotPoleService;

  /**
   * 开灯
   * POST /mqtt/pole/LP001/on
   */
  @PostMapping("/pole/{poleCode}/on")
  public AjaxResult turnOn(@PathVariable String poleCode)
  {
    // 检查MQTT连接状态
    if (!mqttPublishService.isConnected())
    {
      return AjaxResult.error("MQTT 服务未连接，请检查 EMQX 是否运行");
    }

    boolean success = mqttPublishService.sendPoleCommand(poleCode, "on", null);
    return success ? AjaxResult.success("开灯命令已发送")
      : AjaxResult.error("开灯命令发送失败，请检查设备是否在线");
  }

  /**
   * 关灯
   * POST /mqtt/pole/LP001/off
   */
  @PostMapping("/pole/{poleCode}/off")
  public AjaxResult turnOff(@PathVariable String poleCode)
  {
    if (!mqttPublishService.isConnected())
    {
      return AjaxResult.error("MQTT 服务未连接，请检查 EMQX 是否运行");
    }

    boolean success = mqttPublishService.sendPoleCommand(poleCode, "off", null);
    return success ? AjaxResult.success("关灯命令已发送")
      : AjaxResult.error("关灯命令发送失败，请检查设备是否在线");
  }

  /**
   * 切换灯状态（取反）
   * POST /mqtt/pole/LP001/toggle
   */
  @PostMapping("/pole/{poleCode}/toggle")
  public AjaxResult toggle(@PathVariable String poleCode)
  {
    if (!mqttPublishService.isConnected())
    {
      return AjaxResult.error("MQTT 服务未连接，请检查 EMQX 是否运行");
    }

    // 从数据库获取当前状态
    String currentStatus = iotPoleService.getLedStatusByPoleCode(poleCode);
    if (currentStatus == null)
    {
      return AjaxResult.error("灯杆不存在");
    }

    String command = "0".equals(currentStatus) ? "on" : "off";
    boolean success = mqttPublishService.sendPoleCommand(poleCode, command, null);
    return success ? AjaxResult.success("切换命令已发送")
      : AjaxResult.error("切换命令发送失败，请检查设备是否在线");
  }

  /**
   * 获取灯杆当前状态（用于前端按钮同步）
   * GET /mqtt/pole/LP001/status
   */
  @GetMapping("/pole/{poleCode}/status")
  public AjaxResult getStatus(@PathVariable String poleCode)
  {
    Map<String, Object> status = iotPoleService.getCurrentStatusByPoleCode(poleCode);
    if (status == null)
    {
      return AjaxResult.error("灯杆不存在");
    }

    Map<String, Object> result = new HashMap<>();
    result.put("poleCode", poleCode);
    result.put("ledStatus", status.get("ledStatus"));
    result.put("mqttConnected", mqttPublishService.isConnected());
    result.put("lastUpdateTime", status.get("updateTime"));
    return AjaxResult.success(result);
  }

  /**
   * 调光（需 ESP32 固件支持 PWM）
   * POST /mqtt/pole/LP001/dim?brightness=80
   *
   * 注意：当前 ESP32 固件暂不支持调光，此接口预留
   */
  @PostMapping("/pole/{poleCode}/dim")
  public AjaxResult dim(@PathVariable String poleCode, @RequestParam Integer brightness)
  {
    if (brightness < 0 || brightness > 100)
    {
      return AjaxResult.error("亮度值必须在 0-100 之间");
    }

    // TODO: 等待 ESP32 固件支持 PWM 调光后启用
    // 当前返回提示信息
    return AjaxResult.warn("调光功能暂未支持，请等待固件升级（需ESP32支持PWM）");

        /*
        if (!mqttPublishService.isConnected())
        {
            return AjaxResult.error("MQTT 服务未连接，请检查 EMQX 是否运行");
        }
        boolean success = mqttPublishService.sendPoleCommand(poleCode, "dim", brightness);
        return success ? AjaxResult.success("调光命令已发送")
                       : AjaxResult.error("调光命令发送失败");
        */
  }

  /**
   * 重启设备
   * POST /mqtt/pole/{poleCode}/restart
   */
  @PostMapping("/pole/{poleCode}/restart")
  public AjaxResult restart(@PathVariable String poleCode)
  {
    if (!mqttPublishService.isConnected())
    {
      return AjaxResult.error("MQTT 服务未连接，请检查 EMQX 是否运行");
    }

    boolean success = mqttPublishService.sendPoleCommand(poleCode, "restart", null);
    return success ? AjaxResult.success("重启命令已发送")
      : AjaxResult.error("重启命令发送失败");
  }

  /**
   * 触发摄像头抓拍（通过MQTT）
   * POST /mqtt/camera/{cameraCode}/capture
   */
  @PostMapping("/camera/{cameraCode}/capture")
  public AjaxResult capture(@PathVariable String cameraCode)
  {
    if (!mqttPublishService.isConnected())
    {
      return AjaxResult.error("MQTT 服务未连接，请检查 EMQX 是否运行");
    }

    boolean success = mqttPublishService.triggerCameraCapture(cameraCode);
    return success ? AjaxResult.success("抓拍命令已发送")
      : AjaxResult.error("抓拍命令发送失败");
  }

  /**
   * 批量控制灯杆
   * POST /mqtt/pole/batch/control
   * Request Body: {"poleCodes":["LP001","LP002"], "action":"on"}
   */
  @PostMapping("/pole/batch/control")
  public AjaxResult batchControl(@RequestBody Map<String, Object> params)
  {
    @SuppressWarnings("unchecked")
    java.util.List<String> poleCodes = (java.util.List<String>) params.get("poleCodes");
    String action = (String) params.get("action");

    if (poleCodes == null || poleCodes.isEmpty())
    {
      return AjaxResult.error("请指定灯杆列表");
    }
    if (!"on".equals(action) && !"off".equals(action))
    {
      return AjaxResult.error("操作类型仅支持 on/off");
    }
    if (!mqttPublishService.isConnected())
    {
      return AjaxResult.error("MQTT 服务未连接，请检查 EMQX 是否运行");
    }

    int successCount = 0;
    for (String poleCode : poleCodes)
    {
      boolean success = mqttPublishService.sendPoleCommand(poleCode, action, null);
      if (success) successCount++;
    }

    return AjaxResult.success(String.format("批量命令已发送，成功 %d / %d 个", successCount, poleCodes.size()));
  }
}
