<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-blue">
            <i class="el-icon-device"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-label">设备总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-cyan">
            <i class="el-icon-camera"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ cameraCount }}</div>
            <div class="stat-label">摄像头</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-green">
            <i class="el-icon-data-line"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ sensorCount }}</div>
            <div class="stat-label">传感器</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-yellow">
            <i class="el-icon-lightning"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ ledCount }}</div>
            <div class="stat-label">LED灯</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb20">
      <el-col :span="12">
        <el-card class="chart-card">
          <div class="chart-title">设备类型分布</div>
          <div ref="typeChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <div class="chart-title">设备状态统计</div>
          <div ref="statusChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="灯杆ID" prop="poleId">
        <el-input
          v-model="queryParams.poleId"
          placeholder="请输入灯杆ID"
          clearable
          @keyup.enter.native="handleQuery"
          style="width: 120px"
        />
      </el-form-item>
      <el-form-item label="设备名称" prop="deviceName">
        <el-input
          v-model="queryParams.deviceName"
          placeholder="请输入设备名称"
          clearable
          @keyup.enter.native="handleQuery"
          style="width: 140px"
        />
      </el-form-item>
      <el-form-item label="设备类型" prop="deviceType">
        <el-select v-model="queryParams.deviceType" placeholder="全部类型" clearable style="width: 120px">
          <el-option label="温湿度" :value="0" />
          <el-option label="光照" :value="1" />
          <el-option label="电压电流" :value="2" />
          <el-option label="LED灯" :value="3" />
          <el-option label="摄像头" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 100px">
          <el-option label="正常" :value="0" />
          <el-option label="离线" :value="1" />
          <el-option label="故障" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['IotDevice:IotDevice:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['IotDevice:IotDevice:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['IotDevice:IotDevice:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['IotDevice:IotDevice:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-card class="table-card">
      <el-table v-loading="loading" :data="IotDeviceList" @selection-change="handleSelectionChange" stripe border>
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="设备编号" align="center" prop="deviceId" width="100" />
        <el-table-column label="灯杆ID" align="center" prop="poleId" width="100" />
        <el-table-column label="设备名称" align="center" prop="deviceName" width="120" />
        <el-table-column label="设备类型" align="center" width="120">
          <template slot-scope="scope">
            <el-tag :type="deviceTypeTagType(scope.row.deviceType)" size="small">
              {{ deviceTypeLabel(scope.row.deviceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="设备型号" align="center" prop="deviceModel" width="140" />
        <el-table-column label="设备状态" align="center" width="100">
          <template slot-scope="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="small">
              {{ statusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)"
              v-hasPermi="['IotDevice:IotDevice:query']"
            >详情</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['IotDevice:IotDevice:edit']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['IotDevice:IotDevice:remove']"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <pagination
        v-show="total>0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="灯杆ID" prop="poleId">
              <el-input v-model="form.poleId" placeholder="请输入灯杆ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="deviceName">
              <el-input v-model="form.deviceName" placeholder="请输入设备名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型" prop="deviceType">
              <el-select v-model="form.deviceType" placeholder="请选择设备类型">
                <el-option label="温湿度" :value="0" />
                <el-option label="光照" :value="1" />
                <el-option label="电压电流" :value="2" />
                <el-option label="LED灯" :value="3" />
                <el-option label="摄像头" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备型号" prop="deviceModel">
              <el-input v-model="form.deviceModel" placeholder="请输入设备型号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择设备状态">
                <el-option label="正常" :value="0" />
                <el-option label="离线" :value="1" />
                <el-option label="故障" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注信息" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="设备详情" :visible.sync="viewOpen" width="550px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="设备编号">{{ form.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="灯杆ID">{{ form.poleId }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ form.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">
          <el-tag :type="deviceTypeTagType(form.deviceType)" size="small">
            {{ deviceTypeLabel(form.deviceType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="设备型号">{{ form.deviceModel }}</el-descriptions-item>
        <el-descriptions-item label="设备状态">
          <el-tag :type="statusTagType(form.status)" size="small">
            {{ statusLabel(form.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(form.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ parseTime(form.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注信息" :span="2">{{ form.remark }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { listIotDevice, getIotDevice, delIotDevice, addIotDevice, updateIotDevice } from "@/api/IotDevice/IotDevice"

export default {
  name: "IotDevice",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      IotDeviceList: [],
      title: "",
      open: false,
      viewOpen: false,
      totalCount: 0,
      cameraCount: 0,
      sensorCount: 0,
      ledCount: 0,
      typeChart: null,
      statusChart: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        poleId: null,
        deviceName: null,
        deviceType: null,
        deviceModel: null,
        status: null,
      },
      form: {},
      rules: {
        poleId: [
          { required: true, message: "所属灯杆ID不能为空", trigger: "blur" }
        ],
        deviceName: [
          { required: true, message: "设备名称不能为空", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  mounted() {
    this.$nextTick(() => {
      this.initCharts()
    })
  },
  beforeDestroy() {
    if (this.typeChart) {
      this.typeChart.dispose()
    }
    if (this.statusChart) {
      this.statusChart.dispose()
    }
  },
  methods: {
    getList() {
      this.loading = true
      listIotDevice(this.queryParams).then(response => {
        this.IotDeviceList = response.rows
        this.total = response.total
        this.loading = false
        this.updateStats()
        this.updateCharts()
      })
    },
    updateStats() {
      this.totalCount = this.total
      this.cameraCount = this.IotDeviceList.filter(item => item.deviceType === 4).length
      this.sensorCount = this.IotDeviceList.filter(item => item.deviceType <= 2).length
      this.ledCount = this.IotDeviceList.filter(item => item.deviceType === 3).length
    },
    initCharts() {
      this.typeChart = echarts.init(this.$refs.typeChart)
      this.statusChart = echarts.init(this.$refs.statusChart)
      this.updateCharts()
    },
    updateCharts() {
      if (!this.typeChart || !this.statusChart) return

      const typeData = [
        { name: '温湿度', value: this.IotDeviceList.filter(item => item.deviceType === 0).length, color: '#409EFF' },
        { name: '光照', value: this.IotDeviceList.filter(item => item.deviceType === 1).length, color: '#67C23A' },
        { name: '电压电流', value: this.IotDeviceList.filter(item => item.deviceType === 2).length, color: '#E6A23C' },
        { name: 'LED灯', value: this.IotDeviceList.filter(item => item.deviceType === 3).length, color: '#F56C6C' },
        { name: '摄像头', value: this.IotDeviceList.filter(item => item.deviceType === 4).length, color: '#909399' }
      ]

      this.typeChart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: typeData.map(item => item.name),
          axisTick: { show: false },
          axisLine: { lineStyle: { color: '#ddd' } }
        },
        yAxis: {
          type: 'value',
          axisTick: { show: false },
          axisLine: { show: false },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        series: [{
          type: 'bar',
          barWidth: '45%',
          data: typeData.map(item => ({
            value: item.value,
            itemStyle: { color: item.color, borderRadius: [4, 4, 0, 0] }
          }))
        }]
      })

      const normal = this.IotDeviceList.filter(item => item.status === 0).length
      const offline = this.IotDeviceList.filter(item => item.status === 1).length
      const fault = this.IotDeviceList.filter(item => item.status === 2).length

      this.statusChart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          bottom: 10,
          left: 'center'
        },
        series: [{
          name: '设备状态',
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: false,
          label: {
            show: true,
            formatter: '{b}\n{d}%'
          },
          data: [
            { value: normal, name: '正常', itemStyle: { color: '#13ce66' } },
            { value: offline, name: '离线', itemStyle: { color: '#ff9800' } },
            { value: fault, name: '故障', itemStyle: { color: '#ff4949' } }
          ]
        }]
      })
    },
    cancel() {
      this.open = false
      this.viewOpen = false
      this.reset()
    },
    reset() {
      this.form = {
        deviceId: null,
        poleId: null,
        deviceName: null,
        deviceType: 0,
        deviceModel: null,
        status: 0,
        delFlag: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null
      }
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.deviceId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加设备"
    },
    handleUpdate(row) {
      this.reset()
      const deviceId = row.deviceId || this.ids
      getIotDevice(deviceId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改设备"
      })
    },
    handleView(row) {
      this.reset()
      getIotDevice(row.deviceId).then(response => {
        this.form = response.data
        this.viewOpen = true
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.deviceId != null) {
            updateIotDevice(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addIotDevice(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const deviceIds = row.deviceId || this.ids
      this.$modal.confirm('是否确认删除设备编号为"' + deviceIds + '"的数据项？').then(function() {
        return delIotDevice(deviceIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('IotDevice/IotDevice/export', {
        ...this.queryParams
      }, `IotDevice_${new Date().getTime()}.xlsx`)
    },
    deviceTypeLabel(type) {
      const map = { 0: '温湿度', 1: '光照', 2: '电压电流', 3: 'LED灯', 4: '摄像头' }
      return map[type] || '未知'
    },
    deviceTypeTagType(type) {
      const map = { 0: 'primary', 1: 'success', 2: 'warning', 3: 'danger', 4: 'info' }
      return map[type] || 'info'
    },
    statusLabel(status) {
      const map = { 0: '正常', 1: '离线', 2: '故障' }
      return map[status] || '未知'
    },
    statusTagType(status) {
      const map = { 0: 'success', 1: 'warning', 2: 'danger' }
      return map[status] || 'info'
    }
  }
}
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 28px;
}

.stat-icon.bg-blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.stat-icon.bg-cyan {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
}

.stat-icon.bg-green {
  background: linear-gradient(135deg, #56ab2f 0%, #a8e063 100%);
  color: #fff;
}

.stat-icon.bg-yellow {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.chart-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  padding-left: 5px;
}

.chart-container {
  height: 220px;
}

.table-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}
</style>
