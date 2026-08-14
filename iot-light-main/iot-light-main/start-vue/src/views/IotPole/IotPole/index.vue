<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-blue">
            <i class="el-icon-lamp"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-label">灯杆总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-green">
            <i class="el-icon-circle-check"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ normalCount }}</div>
            <div class="stat-label">正常运行</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-orange">
            <i class="el-icon-offline"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ offlineCount }}</div>
            <div class="stat-label">离线设备</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-red">
            <i class="el-icon-warning"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ faultCount }}</div>
            <div class="stat-label">故障告警</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb20">
      <el-col :span="8">
        <el-card class="chart-card">
          <div class="chart-title">灯杆状态分布</div>
          <div ref="statusChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card class="chart-card">
          <div class="chart-title">LED灯状态统计</div>
          <div ref="ledChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="灯杆编号" prop="poleCode">
        <el-input
          v-model="queryParams.poleCode"
          placeholder="请输入灯杆编号"
          clearable
          @keyup.enter.native="handleQuery"
          style="width: 140px"
        />
      </el-form-item>
      <el-form-item label="灯杆名称" prop="poleName">
        <el-input
          v-model="queryParams.poleName"
          placeholder="请输入灯杆名称"
          clearable
          @keyup.enter.native="handleQuery"
          style="width: 140px"
        />
      </el-form-item>
      <el-form-item label="安装位置" prop="location">
        <el-input
          v-model="queryParams.location"
          placeholder="请输入安装位置"
          clearable
          @keyup.enter.native="handleQuery"
          style="width: 180px"
        />
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
          v-hasPermi="['IotPole:IotPole:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['IotPole:IotPole:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['IotPole:IotPole:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['IotPole:IotPole:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-card class="table-card">
      <el-table v-loading="loading" :data="IotPoleList" @selection-change="handleSelectionChange" stripe border>
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="灯杆编号" align="center" prop="poleCode" width="120" />
        <el-table-column label="灯杆名称" align="center" prop="poleName" width="120" />
        <el-table-column label="安装位置" align="center" prop="location" show-overflow-tooltip width="180" />
        <el-table-column label="坐标位置" align="center" width="160">
          <template slot-scope="scope">
            <span>{{ scope.row.latitude }}, {{ scope.row.longitude }}</span>
          </template>
        </el-table-column>
        <el-table-column label="灯杆状态" align="center" width="100">
          <template slot-scope="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="small">
              {{ statusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="LED状态" align="center" width="100">
          <template slot-scope="scope">
            <el-tag :type="ledTagType(scope.row.ledStatus)" size="small">
              {{ ledLabel(scope.row.ledStatus) }}
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
              v-hasPermi="['IotPole:IotPole:query']"
            >详情</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['IotPole:IotPole:edit']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['IotPole:IotPole:remove']"
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
            <el-form-item label="灯杆编号" prop="poleCode">
              <el-input v-model="form.poleCode" placeholder="请输入灯杆编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="灯杆名称" prop="poleName">
              <el-input v-model="form.poleName" placeholder="请输入灯杆名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="安装位置" prop="location">
              <el-input v-model="form.location" placeholder="请输入安装位置描述" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度坐标" prop="latitude">
              <el-input v-model="form.latitude" placeholder="请输入纬度" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度坐标" prop="longitude">
              <el-input v-model="form.longitude" placeholder="请输入经度" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="灯杆状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态">
                <el-option label="正常" :value="0" />
                <el-option label="离线" :value="1" />
                <el-option label="故障" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="LED状态" prop="ledStatus">
              <el-select v-model="form.ledStatus" placeholder="请选择LED状态">
                <el-option label="关闭" :value="0" />
                <el-option label="开启" :value="1" />
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

    <el-dialog title="灯杆详情" :visible.sync="viewOpen" width="550px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="灯杆编号">{{ form.poleCode }}</el-descriptions-item>
        <el-descriptions-item label="灯杆名称">{{ form.poleName }}</el-descriptions-item>
        <el-descriptions-item label="安装位置">{{ form.location }}</el-descriptions-item>
        <el-descriptions-item label="坐标位置">{{ form.latitude }}, {{ form.longitude }}</el-descriptions-item>
        <el-descriptions-item label="灯杆状态">
          <el-tag :type="statusTagType(form.status)" size="small">
            {{ statusLabel(form.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="LED状态">
          <el-tag :type="ledTagType(form.ledStatus)" size="small">
            {{ ledLabel(form.ledStatus) }}
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
import { listIotPole, getIotPole, delIotPole, addIotPole, updateIotPole } from "@/api/IotPole/IotPole"

export default {
  name: "IotPole",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      IotPoleList: [],
      title: "",
      open: false,
      viewOpen: false,
      totalCount: 0,
      normalCount: 0,
      offlineCount: 0,
      faultCount: 0,
      statusChart: null,
      ledChart: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        poleCode: null,
        poleName: null,
        location: null,
        latitude: null,
        longitude: null,
        status: null,
        ledStatus: null,
        orderNum: null,
      },
      form: {},
      rules: {
        poleCode: [
          { required: true, message: "灯杆编号不能为空", trigger: "blur" }
        ],
        poleName: [
          { required: true, message: "灯杆名称不能为空", trigger: "blur" }
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
    if (this.statusChart) {
      this.statusChart.dispose()
    }
    if (this.ledChart) {
      this.ledChart.dispose()
    }
  },
  methods: {
    getList() {
      this.loading = true
      listIotPole(this.queryParams).then(response => {
        this.IotPoleList = response.rows
        this.total = response.total
        this.loading = false
        this.updateStats()
        this.updateCharts()
      })
    },
    updateStats() {
      this.totalCount = this.total
      this.normalCount = this.IotPoleList.filter(item => item.status === 0).length
      this.offlineCount = this.IotPoleList.filter(item => item.status === 1).length
      this.faultCount = this.IotPoleList.filter(item => item.status === 2).length
    },
    initCharts() {
      this.statusChart = echarts.init(this.$refs.statusChart)
      this.ledChart = echarts.init(this.$refs.ledChart)
      this.updateCharts()
    },
    updateCharts() {
      if (!this.statusChart || !this.ledChart) return
      
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
          name: '灯杆状态',
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: false,
          label: {
            show: true,
            formatter: '{b}\n{d}%'
          },
          data: [
            { value: this.normalCount, name: '正常', itemStyle: { color: '#13ce66' } },
            { value: this.offlineCount, name: '离线', itemStyle: { color: '#ff9800' } },
            { value: this.faultCount, name: '故障', itemStyle: { color: '#ff4949' } }
          ]
        }]
      })

      const ledOn = this.IotPoleList.filter(item => item.ledStatus === 1).length
      const ledOff = this.IotPoleList.filter(item => item.ledStatus === 0).length
      
      this.ledChart.setOption({
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
          data: ['LED开启', 'LED关闭'],
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
          barWidth: '50%',
          data: [
            { value: ledOn, itemStyle: { color: '#13ce66', borderRadius: [4, 4, 0, 0] } },
            { value: ledOff, itemStyle: { color: '#909399', borderRadius: [4, 4, 0, 0] } }
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
        poleId: null,
        poleCode: null,
        poleName: null,
        location: null,
        latitude: null,
        longitude: null,
        status: 0,
        ledStatus: 0,
        orderNum: null,
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
      this.ids = selection.map(item => item.poleId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加灯杆"
    },
    handleUpdate(row) {
      this.reset()
      const poleId = row.poleId || this.ids
      getIotPole(poleId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改灯杆"
      })
    },
    handleView(row) {
      this.reset()
      getIotPole(row.poleId).then(response => {
        this.form = response.data
        this.viewOpen = true
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.poleId != null) {
            updateIotPole(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addIotPole(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const poleIds = row.poleId || this.ids
      this.$modal.confirm('是否确认删除灯杆编号为"' + poleIds + '"的数据项？').then(function() {
        return delIotPole(poleIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('IotPole/IotPole/export', {
        ...this.queryParams
      }, `IotPole_${new Date().getTime()}.xlsx`)
    },
    statusLabel(status) {
      const map = { 0: '正常', 1: '离线', 2: '故障' }
      return map[status] || '未知'
    },
    statusTagType(status) {
      const map = { 0: 'success', 1: 'warning', 2: 'danger' }
      return map[status] || 'info'
    },
    ledLabel(status) {
      const map = { 0: '关闭', 1: '开启' }
      return map[status] || '未知'
    },
    ledTagType(status) {
      const map = { 0: 'info', 1: 'success' }
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

.stat-icon.bg-green {
  background: linear-gradient(135deg, #56ab2f 0%, #a8e063 100%);
  color: #fff;
}

.stat-icon.bg-orange {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: #fff;
}

.stat-icon.bg-red {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
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
