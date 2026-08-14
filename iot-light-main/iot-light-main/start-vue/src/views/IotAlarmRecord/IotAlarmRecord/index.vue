<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-red">
            <i class="el-icon-bell"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-label">报警总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-orange">
            <i class="el-icon-clock"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ pendingCount }}</div>
            <div class="stat-label">待处理</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-blue">
            <i class="el-icon-loading"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ processingCount }}</div>
            <div class="stat-label">处理中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-green">
            <i class="el-icon-check-circle"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ handledCount }}</div>
            <div class="stat-label">已处理</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb20">
      <el-col :span="12">
        <el-card class="chart-card">
          <div class="chart-title">报警类型分布</div>
          <div ref="typeChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <div class="chart-title">报警状态统计</div>
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
      <el-form-item label="报警类型" prop="paramType">
        <el-select v-model="queryParams.paramType" placeholder="请选择" clearable style="width: 120px">
          <el-option label="温度" :value="0" />
          <el-option label="湿度" :value="1" />
          <el-option label="光照" :value="2" />
          <el-option label="电压" :value="3" />
          <el-option label="电流" :value="4" />
          <el-option label="行人" :value="5" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 120px">
          <el-option label="未处理" :value="0" />
          <el-option label="已处理" :value="1" />
          <el-option label="已忽略" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理时间" prop="handleTime">
        <el-date-picker
          v-model="queryParams.handleTime"
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
          v-hasPermi="['IotAlarmRecord:IotAlarmRecord:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['IotAlarmRecord:IotAlarmRecord:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['IotAlarmRecord:IotAlarmRecord:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['IotAlarmRecord:IotAlarmRecord:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-card class="table-card">
      <el-table v-loading="loading" :data="IotAlarmRecordList" @selection-change="handleSelectionChange" stripe border>
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="记录主键" align="center" prop="recordId" width="100" />
        <el-table-column label="灯杆ID" align="center" prop="poleId" width="100" />
        <el-table-column label="触发规则" align="center" prop="ruleId" width="100" />
        <el-table-column label="报警类型" align="center" width="100">
          <template slot-scope="scope">
            <el-tag :type="paramTypeTag(scope.row.paramType)" size="small">{{ paramTypeText(scope.row.paramType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="触发数值" align="center" width="120">
          <template slot-scope="scope">
            <span>{{ scope.row.alarmValue }}{{ paramUnit(scope.row.paramType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="报警描述" align="center" prop="alarmMessage" show-overflow-tooltip />
        <el-table-column label="处理状态" align="center" width="100">
          <template slot-scope="scope">
            <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理人" align="center" prop="handleBy" width="100" />
        <el-table-column label="处理时间" align="center" prop="handleTime" width="180">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.handleTime, '{y}-{m}-{d} {h}:{i}:{s}') || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)"
              v-hasPermi="['IotAlarmRecord:IotAlarmRecord:query']"
            >详情</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['IotAlarmRecord:IotAlarmRecord:edit']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['IotAlarmRecord:IotAlarmRecord:remove']"
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

    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="灯杆ID" prop="poleId">
          <el-input v-model="form.poleId" placeholder="请输入灯杆ID" />
        </el-form-item>
        <el-form-item label="规则ID" prop="ruleId">
          <el-input v-model="form.ruleId" placeholder="请输入触发的规则ID" />
        </el-form-item>
        <el-form-item label="报警类型" prop="paramType">
          <el-select v-model="form.paramType" placeholder="请选择报警类型">
            <el-option label="温度" :value="0" />
            <el-option label="湿度" :value="1" />
            <el-option label="光照" :value="2" />
            <el-option label="电压" :value="3" />
            <el-option label="电流" :value="4" />
            <el-option label="行人" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发数值" prop="alarmValue">
          <el-input v-model="form.alarmValue" placeholder="请输入触发时的数值" />
        </el-form-item>
        <el-form-item label="报警描述" prop="alarmMessage">
          <el-input v-model="form.alarmMessage" type="textarea" placeholder="请输入报警描述信息" :rows="3" />
        </el-form-item>
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择处理状态">
            <el-option label="未处理" :value="0" />
            <el-option label="已处理" :value="1" />
            <el-option label="已忽略" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人" prop="handleBy">
          <el-input v-model="form.handleBy" placeholder="请输入处理人" />
        </el-form-item>
        <el-form-item label="处理时间" prop="handleTime">
          <el-date-picker
            v-model="form.handleTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="选择处理时间"
          />
        </el-form-item>
        <el-form-item label="处理备注" prop="handleRemark">
          <el-input v-model="form.handleRemark" type="textarea" placeholder="请输入处理备注" :rows="3" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="报警记录详情" :visible.sync="viewOpen" width="550px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="记录主键">{{ form.recordId }}</el-descriptions-item>
        <el-descriptions-item label="灯杆ID">{{ form.poleId }}</el-descriptions-item>
        <el-descriptions-item label="触发规则">{{ form.ruleId }}</el-descriptions-item>
        <el-descriptions-item label="报警类型">
          <el-tag :type="paramTypeTag(form.paramType)" size="small">{{ paramTypeText(form.paramType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="触发数值">
          {{ form.alarmValue }}{{ paramUnit(form.paramType) }}
        </el-descriptions-item>
        <el-descriptions-item label="处理状态">
          <el-tag :type="statusTag(form.status)" size="small">{{ statusText(form.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报警描述" :span="2">
          {{ form.alarmMessage || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="处理人">{{ form.handleBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ parseTime(form.handleTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理备注" :span="2">
          {{ form.handleRemark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(form.createTime) }}</el-descriptions-item>
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
import { listIotAlarmRecord, getIotAlarmRecord, delIotAlarmRecord, addIotAlarmRecord, updateIotAlarmRecord } from "@/api/IotAlarmRecord/IotAlarmRecord"

export default {
  name: "IotAlarmRecord",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      IotAlarmRecordList: [],
      title: "",
      open: false,
      viewOpen: false,
      totalCount: 0,
      pendingCount: 0,
      processingCount: 0,
      handledCount: 0,
      typeChart: null,
      statusChart: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        poleId: null,
        ruleId: null,
        paramType: null,
        alarmValue: null,
        alarmMessage: null,
        status: null,
        handleBy: null,
        handleTime: null,
        handleRemark: null,
      },
      form: {},
      rules: {
        poleId: [
          { required: true, message: "灯杆ID不能为空", trigger: "blur" }
        ],
        paramType: [
          { required: true, message: "报警参数类型不能为空", trigger: "change" }
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
    if (this.statusChart) this.statusChart.dispose()
  },
  methods: {
    getList() {
      this.loading = true
      listIotAlarmRecord(this.queryParams).then(response => {
        this.IotAlarmRecordList = response.rows
        this.total = response.total
        this.loading = false
        this.updateStats()
        this.updateCharts()
      })
    },
    updateStats() {
      this.totalCount = this.total
      this.pendingCount = this.IotAlarmRecordList.filter(item => item.status === 0).length
      this.processingCount = this.IotAlarmRecordList.filter(item => item.status === 1).length
      this.handledCount = this.IotAlarmRecordList.filter(item => item.status === 2).length
    },
    initCharts() {
      this.typeChart = echarts.init(this.$refs.typeChart)
      this.statusChart = echarts.init(this.$refs.statusChart)
      this.updateCharts()
    },
    updateCharts() {
      const typeCounts = { 0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 }
      this.IotAlarmRecordList.forEach(item => {
        if (typeCounts.hasOwnProperty(item.paramType)) {
          typeCounts[item.paramType]++
        }
      })

      this.typeChart.setOption({
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
          data: ['温度', '湿度', '光照', '电压', '电流', '行人'],
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
              { value: typeCounts[0], itemStyle: { color: '#f56c6c' } },
              { value: typeCounts[1], itemStyle: { color: '#409EFF' } },
              { value: typeCounts[2], itemStyle: { color: '#e6a23c' } },
              { value: typeCounts[3], itemStyle: { color: '#67c23a' } },
              { value: typeCounts[4], itemStyle: { color: '#909399' } },
              { value: typeCounts[5], itemStyle: { color: '#b37feb' } }
            ],
            type: 'bar',
            barWidth: '40%',
            borderRadius: [6, 6, 0, 0]
          }
        ]
      })

      this.statusChart.setOption({
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
            name: '处理状态',
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
              { value: this.pendingCount, name: '未处理', itemStyle: { color: '#e6a23c' } },
              { value: this.processingCount, name: '已处理', itemStyle: { color: '#13ce66' } },
              { value: this.handledCount, name: '已忽略', itemStyle: { color: '#909399' } }
            ]
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
        recordId: null,
        poleId: null,
        ruleId: null,
        paramType: null,
        alarmValue: null,
        alarmMessage: null,
        status: null,
        handleBy: null,
        handleTime: null,
        handleRemark: null,
        delFlag: null,
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
      this.ids = selection.map(item => item.recordId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加报警记录"
    },
    handleUpdate(row) {
      this.reset()
      const recordId = row.recordId || this.ids
      getIotAlarmRecord(recordId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改报警记录"
      })
    },
    handleView(row) {
      this.reset()
      getIotAlarmRecord(row.recordId).then(response => {
        this.form = response.data
        this.viewOpen = true
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.recordId != null) {
            updateIotAlarmRecord(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addIotAlarmRecord(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const recordIds = row.recordId || this.ids
      this.$modal.confirm('是否确认删除报警记录编号为"' + recordIds + '"的数据项？').then(function() {
        return delIotAlarmRecord(recordIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('IotAlarmRecord/IotAlarmRecord/export', {
        ...this.queryParams
      }, `IotAlarmRecord_${new Date().getTime()}.xlsx`)
    },
    paramTypeText(type) {
      const map = { 0: '温度', 1: '湿度', 2: '光照', 3: '电压', 4: '电流', 5: '行人' }
      return map[type] || '未知'
    },
    paramTypeTag(type) {
      const map = { 0: 'danger', 1: 'primary', 2: 'warning', 3: 'success', 4: 'info', 5: 'purple' }
      return map[type] || 'info'
    },
    paramUnit(type) {
      const map = { 0: '°C', 1: '%', 2: 'lux', 3: 'V', 4: 'A', 5: '人' }
      return map[type] || ''
    },
    statusText(status) {
      const map = { 0: '未处理', 1: '已处理', 2: '已忽略' }
      return map[status] || '未知'
    },
    statusTag(status) {
      const map = { 0: 'warning', 1: 'success', 2: 'info' }
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

.stat-icon.bg-red {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: #fff;
}

.stat-icon.bg-orange {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: #fff;
}

.stat-icon.bg-blue {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
}

.stat-icon.bg-green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
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
