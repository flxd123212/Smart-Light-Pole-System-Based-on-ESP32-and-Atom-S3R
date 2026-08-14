<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-blue">
            <i class="el-icon-video-play"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-label">控制总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-green">
            <i class="el-icon-circle-check"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ successCount }}</div>
            <div class="stat-label">执行成功</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-red">
            <i class="el-icon-circle-close"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ failCount }}</div>
            <div class="stat-label">执行失败</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-purple">
            <i class="el-icon-percentage"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ successRate }}%</div>
            <div class="stat-label">成功率</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb20">
      <el-col :span="12">
        <el-card class="chart-card">
          <div class="chart-title">控制类型分布</div>
          <div ref="typeChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <div class="chart-title">执行结果统计</div>
          <div ref="resultChart" class="chart-container"></div>
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
      <el-form-item label="设备ID" prop="deviceId">
        <el-input
          v-model="queryParams.deviceId"
          placeholder="请输入设备ID"
          clearable
          @keyup.enter.native="handleQuery"
          style="width: 120px"
        />
      </el-form-item>
      <el-form-item label="控制类型" prop="controlType">
        <el-select v-model="queryParams.controlType" placeholder="请选择" clearable style="width: 120px">
          <el-option label="开灯" :value="0" />
          <el-option label="关灯" :value="1" />
          <el-option label="重启" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="执行结果" prop="result">
        <el-select v-model="queryParams.result" placeholder="请选择" clearable style="width: 100px">
          <el-option label="成功" :value="0" />
          <el-option label="失败" :value="1" />
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
          v-hasPermi="['IotControlLog:IotControlLog:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['IotControlLog:IotControlLog:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['IotControlLog:IotControlLog:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['IotControlLog:IotControlLog:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-card class="table-card">
      <el-table v-loading="loading" :data="IotControlLogList" @selection-change="handleSelectionChange" stripe border>
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="日志主键" align="center" prop="logId" width="100" />
        <el-table-column label="灯杆ID" align="center" prop="poleId" width="100" />
        <el-table-column label="设备ID" align="center" prop="deviceId" width="100" />
        <el-table-column label="控制类型" align="center" width="100">
          <template slot-scope="scope">
            <el-tag :type="controlTypeTag(scope.row.controlType)" size="small">{{ controlTypeText(scope.row.controlType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="执行结果" align="center" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.result === 0 ? 'success' : 'danger'" size="small">
              {{ scope.row.result === 0 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="错误信息" align="center" prop="errorMessage" show-overflow-tooltip />
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
              v-hasPermi="['IotControlLog:IotControlLog:query']"
            >详情</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['IotControlLog:IotControlLog:edit']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['IotControlLog:IotControlLog:remove']"
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

    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="灯杆ID" prop="poleId">
          <el-input v-model="form.poleId" placeholder="请输入灯杆ID" />
        </el-form-item>
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="form.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="控制类型" prop="controlType">
          <el-select v-model="form.controlType" placeholder="请选择控制类型">
            <el-option label="开灯" :value="0" />
            <el-option label="关灯" :value="1" />
            <el-option label="重启" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行结果" prop="result">
          <el-select v-model="form.result" placeholder="请选择执行结果">
            <el-option label="成功" :value="0" />
            <el-option label="失败" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="错误信息" prop="errorMessage">
          <el-input v-model="form.errorMessage" type="textarea" placeholder="请输入错误信息" :rows="3" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="控制日志详情" :visible.sync="viewOpen" width="500px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="日志主键">{{ form.logId }}</el-descriptions-item>
        <el-descriptions-item label="灯杆ID">{{ form.poleId }}</el-descriptions-item>
        <el-descriptions-item label="设备ID">{{ form.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="控制类型">
          <el-tag :type="controlTypeTag(form.controlType)" size="small">{{ controlTypeText(form.controlType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行结果">
          <el-tag :type="form.result === 0 ? 'success' : 'danger'" size="small">
            {{ form.result === 0 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(form.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2">
          {{ form.errorMessage || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建人">{{ form.createBy || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { listIotControlLog, getIotControlLog, delIotControlLog, addIotControlLog, updateIotControlLog } from "@/api/IotControlLog/IotControlLog"

export default {
  name: "IotControlLog",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      IotControlLogList: [],
      title: "",
      open: false,
      viewOpen: false,
      totalCount: 0,
      successCount: 0,
      failCount: 0,
      successRate: 0,
      typeChart: null,
      resultChart: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        poleId: null,
        deviceId: null,
        controlType: null,
        result: null,
        errorMessage: null,
      },
      form: {},
      rules: {
        poleId: [
          { required: true, message: "灯杆ID不能为空", trigger: "blur" }
        ],
        controlType: [
          { required: true, message: "控制类型不能为空", trigger: "change" }
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
    if (this.typeChart) this.typeChart.dispose()
    if (this.resultChart) this.resultChart.dispose()
  },
  methods: {
    getList() {
      this.loading = true
      listIotControlLog(this.queryParams).then(response => {
        this.IotControlLogList = response.rows
        this.total = response.total
        this.loading = false
        this.updateStats()
        this.updateCharts()
      })
    },
    updateStats() {
      this.totalCount = this.total
      this.successCount = this.IotControlLogList.filter(item => item.result === 0).length
      this.failCount = this.IotControlLogList.filter(item => item.result === 1).length
      this.successRate = this.totalCount > 0 ? Math.round((this.successCount / this.totalCount) * 100) : 0
    },
    initCharts() {
      this.typeChart = echarts.init(this.$refs.typeChart)
      this.resultChart = echarts.init(this.$refs.resultChart)
      this.updateCharts()
    },
    updateCharts() {
      const typeCounts = { 0: 0, 1: 0, 2: 0 }
      this.IotControlLogList.forEach(item => {
        if (typeCounts.hasOwnProperty(item.controlType)) {
          typeCounts[item.controlType]++
        }
      })

      this.typeChart.setOption({
        tooltip: {
          trigger: 'item'
        },
        legend: {
          orient: 'vertical',
          right: '5%',
          top: 'center'
        },
        series: [
          {
            name: '控制类型',
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['40%', '50%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 8,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 16,
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: [
              { value: typeCounts[0], name: '开灯', itemStyle: { color: '#e6a23c' } },
              { value: typeCounts[1], name: '关灯', itemStyle: { color: '#67c23a' } },
              { value: typeCounts[2], name: '重启', itemStyle: { color: '#409EFF' } }
            ]
          }
        ]
      })

      this.resultChart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: ['成功', '失败'],
          axisLine: { lineStyle: { color: '#ddd' } }
        },
        yAxis: {
          type: 'value',
          axisLine: { show: false },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        series: [
          {
            data: [
              { value: this.successCount, itemStyle: { color: '#13ce66' } },
              { value: this.failCount, itemStyle: { color: '#ff4949' } }
            ],
            type: 'bar',
            barWidth: '50%',
            borderRadius: [6, 6, 0, 0]
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
        logId: null,
        poleId: null,
        deviceId: null,
        controlType: null,
        result: null,
        errorMessage: null,
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
      this.ids = selection.map(item => item.logId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加控制日志"
    },
    handleUpdate(row) {
      this.reset()
      const logId = row.logId || this.ids
      getIotControlLog(logId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改控制日志"
      })
    },
    handleView(row) {
      this.reset()
      getIotControlLog(row.logId).then(response => {
        this.form = response.data
        this.viewOpen = true
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.logId != null) {
            updateIotControlLog(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addIotControlLog(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const logIds = row.logId || this.ids
      this.$modal.confirm('是否确认删除控制日志编号为"' + logIds + '"的数据项？').then(function() {
        return delIotControlLog(logIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('IotControlLog/IotControlLog/export', {
        ...this.queryParams
      }, `IotControlLog_${new Date().getTime()}.xlsx`)
    },
    controlTypeText(type) {
      const map = { 0: '开灯', 1: '关灯', 2: '重启' }
      return map[type] || '未知'
    },
    controlTypeTag(type) {
      const map = { 0: 'warning', 1: 'success', 2: 'primary' }
      return map[type] || 'info'
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
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
}

.stat-icon.bg-green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: #fff;
}

.stat-icon.bg-red {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: #fff;
}

.stat-icon.bg-purple {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
  height: 200px;
}

.table-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}
</style>
