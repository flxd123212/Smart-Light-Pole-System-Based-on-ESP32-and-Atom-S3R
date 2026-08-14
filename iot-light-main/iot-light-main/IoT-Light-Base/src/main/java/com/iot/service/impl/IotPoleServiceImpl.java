package com.iot.service.impl;

import java.util.List;
import com.iotlight.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.iot.mapper.IotPoleMapper;
import com.iot.domain.IotPole;
import com.iot.service.IIotPoleService;

/**
 * 灯杆Service业务层处理
 * 
 * @author extrao
 * @date 2026-07-24
 */
@Service
public class IotPoleServiceImpl implements IIotPoleService 
{
    @Autowired
    private IotPoleMapper iotPoleMapper;

    /**
     * 查询灯杆
     * 
     * @param poleId 灯杆主键
     * @return 灯杆
     */
    @Override
    public IotPole selectIotPoleByPoleId(Long poleId)
    {
        return iotPoleMapper.selectIotPoleByPoleId(poleId);
    }

    /**
     * 查询灯杆列表
     * 
     * @param iotPole 灯杆
     * @return 灯杆
     */
    @Override
    public List<IotPole> selectIotPoleList(IotPole iotPole)
    {
        return iotPoleMapper.selectIotPoleList(iotPole);
    }

    /**
     * 新增灯杆
     * 
     * @param iotPole 灯杆
     * @return 结果
     */
    @Override
    public int insertIotPole(IotPole iotPole)
    {
        iotPole.setCreateTime(DateUtils.getNowDate());
        return iotPoleMapper.insertIotPole(iotPole);
    }

    /**
     * 修改灯杆
     * 
     * @param iotPole 灯杆
     * @return 结果
     */
    @Override
    public int updateIotPole(IotPole iotPole)
    {
        iotPole.setUpdateTime(DateUtils.getNowDate());
        return iotPoleMapper.updateIotPole(iotPole);
    }

    /**
     * 批量删除灯杆
     * 
     * @param poleIds 需要删除的灯杆主键
     * @return 结果
     */
    @Override
    public int deleteIotPoleByPoleIds(Long[] poleIds)
    {
        return iotPoleMapper.deleteIotPoleByPoleIds(poleIds);
    }

    /**
     * 删除灯杆信息
     * 
     * @param poleId 灯杆主键
     * @return 结果
     */
    @Override
    public int deleteIotPoleByPoleId(Long poleId)
    {
        return iotPoleMapper.deleteIotPoleByPoleId(poleId);
    }
}
