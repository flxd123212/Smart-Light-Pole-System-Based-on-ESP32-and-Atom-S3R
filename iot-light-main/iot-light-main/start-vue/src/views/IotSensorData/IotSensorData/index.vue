<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-blue">
            <i class="el-icon-data-line"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-label">数据条数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-orange">
            <i class="el-icon-temperature"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ avgTemp }}°C</div>
            <div class="stat-label">平均温度</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-cyan">
            <i class="el-icon-water"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ avgHumidity }}%</div>
            <div class="stat-label">平均湿度</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-yellow">
            <i class="el-icon-sunny"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ avgIllum }}</div>
            <div class="stat-label">平均光照(lux)</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb20">
      <el-col :span="24">
        <el-card class="chart-card">
          <div class="chart-title">传感器数据趋势</div>
          <div ref="trendChart" class="chart-container-lg"></div>
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
      <el-form-item label="采集时间" prop="collectTime">
        <el-date-picker
          v-model="queryParams.collectTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="选择日期"
          clearable
          style="width: 150px"
        />
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
          v-hasPermi="['IotSensorData:IotSensorData:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['IotSensorData:IotSensorData:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['IotSensorData:IotSensorData:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['IotSensorData:IotSensorData:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-card class="table-card">
      <el-table v-loading="loading" :data="IotSensorDataList" @selection-change="handleSelectionChange" stripe border>
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="数据编号" align="center" prop="dataId" width="100" />
        <el-table-column label="灯杆ID" align="center" prop="poleId" width="100" />
        <el-table-column label="温度" align="center" width="100">
          <template slot-scope="scope">
            <span :class="tempClass(scope.row.temperature)">{{ scope.row.temperature }}°C</span>
          </template>
        </el-table-column>
        <el-table-column label="湿度" align="center" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.humidity }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="光照" align="center" width="120">
          <template slot-scope="scope">
            <span>{{ scope.row.illumination }} lux</span>
          </template>
        </el-table-column>
        <el-table-column label="电压" align="center" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.voltage }}V</span>
          </template>
        </el-table-column>
        <el-table-column label="电流" align="center" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.current }}A</span>
          </template>
        </el-table-column>
        <el-table-column label="采集时间" align="center" prop="collectTime" width="180">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.collectTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)"
              v-hasPermi="['IotSensorData:IotSensorData:query']"
            >详情</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['IotSensorData:IotSensorData:edit']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['IotSensorData:IotSensorData:remove']"
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
            <el-form-item label="采集时间" prop="collectTime">
              <el-date-picker
                v-model="form.collectTime"
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss"
                placeholder="选择时间"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="温度(°C)" prop="temperature">
              <el-input-number v-model="form.temperature" :min="-40" :max="120" step="0.1" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="湿度(%)" prop="humidity">
              <el-input-number v-model="form.humidity" :min="0" :max="100" step="0.1" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="光照(lux)" prop="illumination">
              <el-input-number v-model="form.illumination" :min="0" :max="100000" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="电压(V)" prop="voltage">
              <el-input-number v-model="form.voltage" :min="0" :max="1000" step="0.1" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="电流(A)" prop="current">
              <el-input-number v-model="form.current" :min="0" :max="100" step="0.01" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="传感器数据详情" :visible.sync="viewOpen" width="500px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="数据编号">{{ form.dataId }}</el-descriptions-item>
        <el-descriptions-item label="灯杆ID">{{ form.poleId }}</el-descriptions-item>
        <el-descriptions-item label="温度">
          <span :class="tempClass(form.temperature)">{{ form.temperature }}°C</span>
        </el-descriptions-item>
        <el-descriptions-item label="湿度">{{ form.humidity }}%</el-descriptions-item>
        <el-descriptions-item label="光照">{{ form.illumination }} lux</el-descriptions-item>
        <el-descriptions-item label="电压">{{ form.voltage }}V</el-descriptions-item>
        <el-descriptions-item label="电流">{{ form.current }}A</el-descriptions-item>
        <el-descriptions-item label="采集时间">{{ parseTime(form.collectTime) }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { listIotSensorData, getIotSensorData, delIotSensorData, addIotSensorData, updateIotSensorData } from "@/api/IotSensorData/IotSensorData"

export default {
  name: "IotSensorData",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      IotSensorDataList: [],
      title: "",
      open: false,
      viewOpen: false,
      totalCount: 0,
      avgTemp: 0,
      avgHumidity: 0,
      avgIllum: 0,
      trendChart: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        poleId: null,
        temperature: null,
        humidity: null,
        illumination: null,
        voltage: null,
        current: null,
        collectTime: null,
      },
      form: {},
      rules: {
        poleId: [
          { required: true, message: "所属灯杆ID不能为空", trigger: "blur" }
        ],
        collectTime: [
          { required: true, message: "数据采集时间不能为空", trigger: "blur" }
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
    if (this.trendChart) {
      this.trendChart.dispose()
    }
  },
  methods: {
    getList() {
      this.loading = true
      listIotSensorData(this.queryParams).then(response => {
        this.IotSensorDataList = response.rows
        this.total = response.total
        this.loading = false
        this.updateStats()
        this.updateCharts()
      })
    },
    updateStats() {
      this.totalCount = this.total
      const temps = this.IotSensorDataList.filter(item => item.temperature).map(item => parseFloat(item.temperature))
      const hums = this.IotSensorDataList.filter(item => item.humidity).map(item => parseFloat(item.humidity))
      const illums = this.IotSensorDataList.filter(item => item.illumination).map(item => parseFloat(item.illumination))
      this.avgTemp = temps.length ? (temps.reduce((a, b) => a + b, 0) / temps.length).toFixed(1) : '-'
      this.avgHumidity = hums.length ? (hums.reduce((a, b) => a + b, 0) / hums.length).toFixed(1) : '-'
      this.avgIllum = illums.length ? Math.round(illums.reduce((a, b) => a + b, 0) / illums.length) : '-'
    },
    initCharts() {
      this.trendChart = echarts.init(this.$refs.trendChart)
      this.updateCharts()
    },
    updateCharts() {
      if (!this.trendChart) return

      const sortedData = [...this.IotSensorDataList].sort((a, b) => new Date(a.collectTime) - new Date(b.collectTime))
      const times = sortedData.map(item => item.collectTime ? parseTime(item.collectTime, '{h}:{i}') : '')
      const temps = sortedData.map(item => parseFloat(item.temperature) || 0)
      const hums = sortedData.map(item => parseFloat(item.humidity) || 0)

      this.trendChart.setOption({
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['温度', '湿度'],
          bottom: 10
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: times,
          axisLine: { lineStyle: { color: '#ddd' } }
        },
        yAxis: [
          {
            type: 'value',
            name: '温度(°C)',
            axisLine: { show: false },
            splitLine: { lineStyle: { color: '#f0f0f0' } }
          },
          {
            type: 'value',
            name: '湿度(%)',
            axisLine: { show: false },
            splitLine: { show: false }
          }
        ],
        series: [
          {
            name: '温度',
            type: 'line',
            smooth: true,
            data: temps,
            lineStyle: { color: '#f56c6c', width: 2 },
            itemStyle: { color: '#f56c6c' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(245, 108, 108, 0.3)' },
                { offset: 1, color: 'rgba(245, 108, 108, 0.05)' }
              ])
            }
          },
          {
            name: '湿度',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            data: hums,
            lineStyle: { color: '#409EFF', width: 2 },
            itemStyle: { color: '#409EFF' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
                { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
              ])
            }
          }
        ]
      })
    },
    cancel() {
      this.open = false
      this.viewOpen = false
      this.reset()
    },
    reset() {
      this.form = {
        dataId: null,
        poleId: null,
        temperature: null,
        humidity: null,
        illumination: null,
        voltage: null,
        current: null,
        collectTime: null,
        createBy: null,
        createTime: null
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
      this.ids = selection.map(item => item.dataId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加传感器数据"
    },
    handleUpdate(row) {
      this.reset()
      const dataId = row.dataId || this.ids
      getIotSensorData(dataId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改传感器数据"
      })
    },
    handleView(row) {
      this.reset()
      getIotSensorData(row.dataId).then(response => {
        this.form = response.data
        this.viewOpen = true
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.dataId != null) {
            updateIotSensorData(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addIotSensorData(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const dataIds = row.dataId || this.ids
      this.$modal.confirm('是否确认删除传感器数据编号为"' + dataIds + '"的数据项？').then(function() {
        return delIotSensorData(dataIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('IotSensorData/IotSensorData/export', {
        ...this.queryParams
      }, `IotSensorData_${new Date().getTime()}.xlsx`)
    },
    tempClass(temp) {
      const t = parseFloat(temp)
      if (t > 35) return 'temp-high'
      if (t < 10) return 'temp-low'
      return 'temp-normal'
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

.stat-icon.bg-orange {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: #fff;
}

.stat-icon.bg-cyan {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
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

.chart-container-lg {
  height: 280px;
}

.table-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.temp-high {
  color: #ff4949;
  font-weight: 600;
}

.temp-low {
  color: #409eff;
  font-weight: 600;
}

.temp-normal {
  color: #13ce66;
  font-weight: 600;
}
</style>
