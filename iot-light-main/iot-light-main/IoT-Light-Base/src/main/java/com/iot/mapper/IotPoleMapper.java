package com.iot.mapper;

import java.util.List;
import com.iot.domain.IotPole;

/**
 * 灯杆Mapper接口
 *
 * @author extrao
 * @date 2026-07-24
 */
public interface IotPoleMapper
{
    /**
     * 查询灯杆
     *
     * @param poleId 灯杆主键
     * @return 灯杆
     */
    public IotPole selectIotPoleByPoleId(Long poleId);

    /**
     * 查询灯杆列表
     *
     * @param iotPole 灯杆
     * @return 灯杆集合
     */
    public List<IotPole> selectIotPoleList(IotPole iotPole);

    /**
     * 新增灯杆
     *
     * @param iotPole 灯杆
     * @return 结果
     */
    public int insertIotPole(IotPole iotPole);

    /**
     * 修改灯杆
     *
     * @param iotPole 灯杆
     * @return 结果
     */
    public int updateIotPole(IotPole iotPole);

    /**
     * 删除灯杆
     *
     * @param poleId 灯杆主键
     * @return 结果
     */
    public int deleteIotPoleByPoleId(Long poleId);

    /**
     * 批量删除灯杆
     *
     * @param poleIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteIotPoleByPoleIds(Long[] poleIds);

    /**
     * 根据灯杆编号查询灯杆
     *
     * @param poleCode 灯杆编号
     * @return 灯杆
     */
    public IotPole selectIotPoleByPoleCode(String poleCode);

    /**
     * 更新灯杆LED状态
     *
     * @param iotPole 灯杆
     * @return 结果
     */
    public int updateIotPoleLedStatus(IotPole iotPole);
}
