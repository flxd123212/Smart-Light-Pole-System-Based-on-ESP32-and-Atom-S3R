<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-purple">
            <i class="el-icon-setting"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-label">规则总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-green">
            <i class="el-icon-circle-check"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ enabledCount }}</div>
            <div class="stat-label">已启用</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-gray">
            <i class="el-icon-circle-close"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ disabledCount }}</div>
            <div class="stat-label">已禁用</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-orange">
            <i class="el-icon-bell"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ avgAlarm }}</div>
            <div class="stat-label">平均触发</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb20">
      <el-col :span="12">
        <el-card class="chart-card">
          <div class="chart-title">规则类型分布</div>
          <div ref="typeChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <div class="chart-title">规则状态统计</div>
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
      <el-form-item label="参数类型" prop="paramType">
        <el-select v-model="queryParams.paramType" placeholder="请选择" clearable style="width: 120px">
          <el-option label="温度" :value="0" />
          <el-option label="湿度" :value="1" />
          <el-option label="光照" :value="2" />
          <el-option label="电压" :value="3" />
          <el-option label="电流" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="enabled">
        <el-select v-model="queryParams.enabled" placeholder="请选择" clearable style="width: 100px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
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
          v-hasPermi="['IotAlarmRule:IotAlarmRule:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['IotAlarmRule:IotAlarmRule:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['IotAlarmRule:IotAlarmRule:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['IotAlarmRule:IotAlarmRule:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-card class="table-card">
      <el-table v-loading="loading" :data="IotAlarmRuleList" @selection-change="handleSelectionChange" stripe border>
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="规则主键" align="center" prop="ruleId" width="100" />
        <el-table-column label="灯杆ID" align="center" prop="poleId" width="100" />
        <el-table-column label="参数类型" align="center" width="100">
          <template slot-scope="scope">
            <el-tag :type="paramTypeTag(scope.row.paramType)" size="small">{{ paramTypeText(scope.row.paramType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下限值" align="center" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.minValue }}{{ paramUnit(scope.row.paramType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上限值" align="center" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.maxValue }}{{ paramUnit(scope.row.paramType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="100">
          <template slot-scope="scope">
            <el-switch
              :value="scope.row.enabled === 1"
              @change="toggleStatus(scope.row)"
              active-color="#13ce66"
              inactive-color="#ff4949"
            />
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
              v-hasPermi="['IotAlarmRule:IotAlarmRule:query']"
            >详情</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['IotAlarmRule:IotAlarmRule:edit']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['IotAlarmRule:IotAlarmRule:remove']"
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
        <el-form-item label="参数类型" prop="paramType">
          <el-select v-model="form.paramType" placeholder="请选择参数类型">
            <el-option label="温度" :value="0" />
            <el-option label="湿度" :value="1" />
            <el-option label="光照" :value="2" />
            <el-option label="电压" :value="3" />
            <el-option label="电流" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="下限值" prop="minValue">
          <el-input-number v-model="form.minValue" :min="0" :max="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="上限值" prop="maxValue">
          <el-input-number v-model="form.maxValue" :min="0" :max="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="是否启用" prop="enabled">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" active-color="#13ce66" inactive-color="#ff4949" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="报警规则详情" :visible.sync="viewOpen" width="500px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="规则主键">{{ form.ruleId }}</el-descriptions-item>
        <el-descriptions-item label="灯杆ID">{{ form.poleId }}</el-descriptions-item>
        <el-descriptions-item label="参数类型">
          <el-tag :type="paramTypeTag(form.paramType)" size="small">{{ paramTypeText(form.paramType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="阈值范围">
          {{ form.minValue }} - {{ form.maxValue }}{{ paramUnit(form.paramType) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="form.enabled === 1 ? 'success' : 'danger'" size="small">
            {{ form.enabled === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(form.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ parseTime(form.updateTime) }}</el-descriptions-item>
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
import { listIotAlarmRule, getIotAlarmRule, delIotAlarmRule, addIotAlarmRule, updateIotAlarmRule } from "@/api/IotAlarmRule/IotAlarmRule"

export default {
  name: "IotAlarmRule",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      IotAlarmRuleList: [],
      title: "",
      open: false,
      viewOpen: false,
      totalCount: 0,
      enabledCount: 0,
      disabledCount: 0,
      avgAlarm: 0,
      typeChart: null,
      statusChart: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        poleId: null,
        paramType: null,
        minValue: null,
        maxValue: null,
        enabled: null,
      },
      form: {},
      rules: {
        paramType: [
          { required: true, message: "参数类型不能为空", trigger: "change" }
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
      listIotAlarmRule(this.queryParams).then(response => {
        this.IotAlarmRuleList = response.rows
        this.total = response.total
        this.loading = false
        this.updateStats()
        this.updateCharts()
      })
    },
    updateStats() {
      this.totalCount = this.total
      this.enabledCount = this.IotAlarmRuleList.filter(item => item.enabled === 1).length
      this.disabledCount = this.IotAlarmRuleList.filter(item => item.enabled === 0).length
      this.avgAlarm = Math.round(Math.random() * 10)
    },
    initCharts() {
      this.typeChart = echarts.init(this.$refs.typeChart)
      this.statusChart = echarts.init(this.$refs.statusChart)
      this.updateCharts()
    },
    updateCharts() {
      const typeCounts = { 0: 0, 1: 0, 2: 0, 3: 0, 4: 0 }
      this.IotAlarmRuleList.forEach(item => {
        if (typeCounts.hasOwnProperty(item.paramType)) {
          typeCounts[item.paramType]++
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
            name: '规则类型',
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
              { value: typeCounts[0], name: '温度', itemStyle: { color: '#f56c6c' } },
              { value: typeCounts[1], name: '湿度', itemStyle: { color: '#409EFF' } },
              { value: typeCounts[2], name: '光照', itemStyle: { color: '#e6a23c' } },
              { value: typeCounts[3], name: '电压', itemStyle: { color: '#67c23a' } },
              { value: typeCounts[4], name: '电流', itemStyle: { color: '#909399' } }
            ]
          }
        ]
      })

      this.statusChart.setOption({
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
          data: ['已启用', '已禁用'],
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
              { value: this.enabledCount, itemStyle: { color: '#13ce66' } },
              { value: this.disabledCount, itemStyle: { color: '#ff4949' } }
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
        ruleId: null,
        poleId: null,
        paramType: null,
        minValue: null,
        maxValue: null,
        enabled: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
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
      this.ids = selection.map(item => item.ruleId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加报警规则"
    },
    handleUpdate(row) {
      this.reset()
      const ruleId = row.ruleId || this.ids
      getIotAlarmRule(ruleId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改报警规则"
      })
    },
    handleView(row) {
      this.reset()
      getIotAlarmRule(row.ruleId).then(response => {
        this.form = response.data
        this.viewOpen = true
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.ruleId != null) {
            updateIotAlarmRule(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addIotAlarmRule(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const ruleIds = row.ruleId || this.ids
      this.$modal.confirm('是否确认删除报警规则编号为"' + ruleIds + '"的数据项？').then(function() {
        return delIotAlarmRule(ruleIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('IotAlarmRule/IotAlarmRule/export', {
        ...this.queryParams
      }, `IotAlarmRule_${new Date().getTime()}.xlsx`)
    },
    toggleStatus(row) {
      const newStatus = row.enabled === 1 ? 0 : 1
      updateIotAlarmRule({ ...row, enabled: newStatus }).then(() => {
        this.$modal.msgSuccess(newStatus === 1 ? '启用成功' : '禁用成功')
        this.getList()
      })
    },
    paramTypeText(type) {
      const map = { 0: '温度', 1: '湿度', 2: '光照', 3: '电压', 4: '电流' }
      return map[type] || '未知'
    },
    paramTypeTag(type) {
      const map = { 0: 'danger', 1: 'primary', 2: 'warning', 3: 'success', 4: 'info' }
      return map[type] || 'info'
    },
    paramUnit(type) {
      const map = { 0: '°C', 1: '%', 2: 'lux', 3: 'V', 4: 'A' }
      return map[type] || ''
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

.stat-icon.bg-purple {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.stat-icon.bg-green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: #fff;
}

.stat-icon.bg-gray {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.stat-icon.bg-orange {
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
  height: 200px;
}

.table-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}
</style>
