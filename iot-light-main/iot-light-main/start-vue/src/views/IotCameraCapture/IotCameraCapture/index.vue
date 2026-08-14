<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-blue">
            <i class="el-icon-camera"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-label">抓拍总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-green">
            <i class="el-icon-user"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalPeople }}</div>
            <div class="stat-label">检测行人</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-orange">
            <i class="el-icon-sort"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ avgPeople }}</div>
            <div class="stat-label">平均行人</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-purple">
            <i class="el-icon-trend-chart"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ maxPeople }}</div>
            <div class="stat-label">峰值行人</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb20">
      <el-col :span="24">
        <el-card class="chart-card">
          <div class="chart-title">行人数量趋势</div>
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
      <el-form-item label="行人数量" prop="personCount">
        <el-input-number v-model="queryParams.personCount" :min="0" :max="100" style="width: 120px" />
      </el-form-item>
      <el-form-item label="抓拍时间" prop="captureTime">
        <el-date-picker
          v-model="queryParams.captureTime"
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
          v-hasPermi="['IotCameraCapture:IotCameraCapture:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['IotCameraCapture:IotCameraCapture:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['IotCameraCapture:IotCameraCapture:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['IotCameraCapture:IotCameraCapture:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-card class="table-card">
      <el-table v-loading="loading" :data="IotCameraCaptureList" @selection-change="handleSelectionChange" stripe border>
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="抓拍主键" align="center" prop="captureId" width="100" />
        <el-table-column label="灯杆ID" align="center" prop="poleId" width="100" />
        <el-table-column label="抓拍图片" align="center" width="120">
          <template slot-scope="scope">
            <el-image
              v-if="scope.row.imageUrl"
              :src="scope.row.imageUrl"
              fit="cover"
              class="capture-img"
              @click="previewImage(scope.row.imageUrl)"
              lazy
            />
            <span v-else class="no-image">暂无图片</span>
          </template>
        </el-table-column>
        <el-table-column label="行人数量" align="center" width="100">
          <template slot-scope="scope">
            <el-tag :type="peopleTag(scope.row.personCount)" size="small">
              {{ scope.row.personCount || 0 }} 人
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="抓拍时间" align="center" prop="captureTime" width="180">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.captureTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)"
              v-hasPermi="['IotCameraCapture:IotCameraCapture:query']"
            >详情</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['IotCameraCapture:IotCameraCapture:edit']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['IotCameraCapture:IotCameraCapture:remove']"
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
        <el-form-item label="灯杆ID" prop="poleId">
          <el-input v-model="form.poleId" placeholder="请输入灯杆ID" />
        </el-form-item>
        <el-form-item label="图片路径" prop="imageUrl">
          <el-input v-model="form.imageUrl" type="textarea" placeholder="请输入图片存储路径" :rows="3" />
        </el-form-item>
        <el-form-item label="行人数量" prop="personCount">
          <el-input-number v-model="form.personCount" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="AI识别结果" prop="resultJson">
          <el-input v-model="form.resultJson" type="textarea" placeholder="请输入AI识别结果JSON" :rows="4" />
        </el-form-item>
        <el-form-item label="抓拍时间" prop="captureTime">
          <el-date-picker
            v-model="form.captureTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="选择抓拍时间"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="抓拍记录详情" :visible.sync="viewOpen" width="600px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="抓拍主键">{{ form.captureId }}</el-descriptions-item>
        <el-descriptions-item label="灯杆ID">{{ form.poleId }}</el-descriptions-item>
        <el-descriptions-item label="行人数量">
          <el-tag :type="peopleTag(form.personCount)" size="small">
            {{ form.personCount || 0 }} 人
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="抓拍时间">{{ parseTime(form.captureTime) }}</el-descriptions-item>
        <el-descriptions-item label="抓拍图片" :span="2">
          <div v-if="form.imageUrl" class="preview-container">
            <el-image :src="form.imageUrl" fit="contain" class="preview-img" @click="previewImage(form.imageUrl)" />
            <span class="preview-tip">点击图片放大查看</span>
          </div>
          <span v-else>暂无图片</span>
        </el-descriptions-item>
        <el-descriptions-item label="AI识别结果" :span="2">
          <pre class="json-pre">{{ form.resultJson || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(form.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ form.createBy || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <el-dialog title="图片预览" :visible.sync="imagePreviewOpen" width="800px" append-to-body>
      <el-image :src="previewImageUrl" fit="contain" class="full-preview-img" />
      <div slot="footer" class="dialog-footer">
        <el-button @click="imagePreviewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { listIotCameraCapture, getIotCameraCapture, delIotCameraCapture, addIotCameraCapture, updateIotCameraCapture } from "@/api/IotCameraCapture/IotCameraCapture"

export default {
  name: "IotCameraCapture",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      IotCameraCaptureList: [],
      title: "",
      open: false,
      viewOpen: false,
      imagePreviewOpen: false,
      previewImageUrl: "",
      totalCount: 0,
      totalPeople: 0,
      avgPeople: 0,
      maxPeople: 0,
      trendChart: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        poleId: null,
        imageUrl: null,
        personCount: null,
        resultJson: null,
        captureTime: null,
      },
      form: {},
      rules: {
        poleId: [
          { required: true, message: "灯杆ID不能为空", trigger: "blur" }
        ],
        captureTime: [
          { required: true, message: "抓拍时间不能为空", trigger: "blur" }
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
      listIotCameraCapture(this.queryParams).then(response => {
        this.IotCameraCaptureList = response.rows
        this.total = response.total
        this.loading = false
        this.updateStats()
        this.updateCharts()
      })
    },
    updateStats() {
      this.totalCount = this.total
      const peopleCounts = this.IotCameraCaptureList.filter(item => item.personCount).map(item => parseInt(item.personCount))
      this.totalPeople = peopleCounts.reduce((a, b) => a + b, 0)
      this.avgPeople = peopleCounts.length ? Math.round(this.totalPeople / peopleCounts.length) : 0
      this.maxPeople = peopleCounts.length ? Math.max(...peopleCounts) : 0
    },
    initCharts() {
      this.trendChart = echarts.init(this.$refs.trendChart)
      this.updateCharts()
    },
    updateCharts() {
      if (!this.trendChart) return

      const sortedData = [...this.IotCameraCaptureList].sort((a, b) => new Date(a.captureTime) - new Date(b.captureTime))
      const times = sortedData.map(item => item.captureTime ? parseTime(item.captureTime, '{h}:{i}') : '')
      const people = sortedData.map(item => parseInt(item.personCount) || 0)

      this.trendChart.setOption({
        tooltip: {
          trigger: 'axis'
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
        yAxis: {
          type: 'value',
          name: '人数',
          min: 0,
          axisLine: { show: false },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        series: [
          {
            name: '行人数量',
            type: 'line',
            smooth: true,
            data: people,
            lineStyle: { color: '#667eea', width: 2 },
            itemStyle: { color: '#667eea' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
                { offset: 1, color: 'rgba(102, 126, 234, 0.05)' }
              ])
            },
            barWidth: '40%'
          }
        ]
      })
    },
    previewImage(url) {
      this.previewImageUrl = url
      this.imagePreviewOpen = true
    },
    cancel() {
      this.open = false
      this.viewOpen = false
      this.imagePreviewOpen = false
      this.reset()
    },
    reset() {
      this.form = {
        captureId: null,
        poleId: null,
        imageUrl: null,
        personCount: null,
        resultJson: null,
        captureTime: null,
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
      this.ids = selection.map(item => item.captureId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加摄像头抓拍记录"
    },
    handleUpdate(row) {
      this.reset()
      const captureId = row.captureId || this.ids
      getIotCameraCapture(captureId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改摄像头抓拍记录"
      })
    },
    handleView(row) {
      this.reset()
      getIotCameraCapture(row.captureId).then(response => {
        this.form = response.data
        this.viewOpen = true
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.captureId != null) {
            updateIotCameraCapture(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addIotCameraCapture(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const captureIds = row.captureId || this.ids
      this.$modal.confirm('是否确认删除摄像头抓拍记录编号为"' + captureIds + '"的数据项？').then(function() {
        return delIotCameraCapture(captureIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('IotCameraCapture/IotCameraCapture/export', {
        ...this.queryParams
      }, `IotCameraCapture_${new Date().getTime()}.xlsx`)
    },
    peopleTag(count) {
      const c = parseInt(count) || 0
      if (c >= 10) return 'danger'
      if (c >= 5) return 'warning'
      if (c > 0) return 'success'
      return 'info'
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

.stat-icon.bg-orange {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
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

.chart-container-lg {
  height: 280px;
}

.table-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.capture-img {
  width: 80px;
  height: 60px;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.capture-img:hover {
  transform: scale(1.1);
}

.no-image {
  color: #909399;
  font-size: 12px;
}

.preview-container {
  position: relative;
}

.preview-img {
  max-height: 200px;
  width: 100%;
  border-radius: 8px;
  cursor: pointer;
}

.preview-tip {
  display: block;
  text-align: center;
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.full-preview-img {
  width: 100%;
  max-height: 500px;
}

.json-pre {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
  font-size: 12px;
  overflow-x: auto;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
