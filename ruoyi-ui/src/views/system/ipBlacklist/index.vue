<template>
  <div class="app-container ip-blacklist-page">
    <el-card shadow="never" class="box-card">
      <div slot="header" class="card-header">
        <div>
          <div class="page-title">IP 黑名单</div>
          <div class="page-subtitle">
            对高风险 IP 进行封禁管理，命中启用状态黑名单的访问请求将被安全过滤器拦截并记录到访问监控日志。
          </div>
        </div>
      </div>

      <el-alert
        title="本地开发保护：127.0.0.1、localhost、IPv6 本地回环地址默认不会被黑名单拦截，避免开发阶段误封自己。"
        type="warning"
        :closable="false"
        show-icon
        class="tips-alert"
      />

      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="78px">
        <el-form-item label="IP地址" prop="ipaddr">
          <el-input
            v-model="queryParams.ipaddr"
            placeholder="请输入IP地址"
            clearable
            style="width: 180px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="封禁原因" prop="reason">
          <el-input
            v-model="queryParams.reason"
            placeholder="请输入封禁原因"
            clearable
            style="width: 220px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="启用" value="0" />
            <el-option label="停用" value="1" />
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
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['system:ipBlacklist:add']"
          >新增</el-button>
        </el-col>

        <el-col :span="1.5">
          <el-button
            type="success"
            plain
            icon="el-icon-edit"
            size="mini"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['system:ipBlacklist:edit']"
          >修改</el-button>
        </el-col>

        <el-col :span="1.5">
          <el-button
            type="danger"
            plain
            icon="el-icon-delete"
            size="mini"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['system:ipBlacklist:remove']"
          >删除</el-button>
        </el-col>

        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="ipBlacklistList" border stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="ID" align="center" prop="blacklistId" width="80" />
        <el-table-column label="IP地址" align="center" prop="ipaddr" width="170">
          <template slot-scope="scope">
            <el-tag type="danger" size="small" effect="plain">{{ scope.row.ipaddr }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="封禁原因" prop="reason" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === '0' || scope.row.status === 0" type="danger" size="small" effect="plain">
              启用
            </el-tag>
            <el-tag v-else type="info" size="small" effect="plain">
              停用
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建者" align="center" prop="createBy" width="120" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="170">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:ipBlacklist:edit']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['system:ipBlacklist:remove']"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="IP地址" prop="ipaddr">
          <el-input v-model="form.ipaddr" placeholder="请输入IP地址，例如 192.168.1.100" />
        </el-form-item>

        <el-form-item label="封禁原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入封禁原因" />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listIpBlacklist, getIpBlacklist, delIpBlacklist, addIpBlacklist, updateIpBlacklist } from '@/api/system/ipBlacklist'

export default {
  name: 'IpBlacklist',
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      ipBlacklistList: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        ipaddr: null,
        reason: null,
        status: null
      },
      form: {},
      rules: {
        ipaddr: [
          { required: true, message: 'IP地址不能为空', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '状态不能为空', trigger: 'change' }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listIpBlacklist(this.queryParams).then(response => {
        this.ipBlacklistList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        blacklistId: null,
        ipaddr: null,
        reason: null,
        status: '0',
        remark: null
      }
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.blacklistId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增IP黑名单'
    },
    handleUpdate(row) {
      this.reset()
      const blacklistId = row.blacklistId || this.ids
      getIpBlacklist(blacklistId).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改IP黑名单'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (this.form.blacklistId != null) {
            updateIpBlacklist(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addIpBlacklist(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const blacklistIds = row.blacklistId || this.ids
      this.$confirm('是否确认删除IP黑名单编号为"' + blacklistIds + '"的数据项？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function() {
        return delIpBlacklist(blacklistIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      })
    }
  }
}
</script>

<style scoped>
.ip-blacklist-page {
  padding: 20px;
}

.box-card {
  border-radius: 6px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  line-height: 26px;
}

.page-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.tips-alert {
  margin-bottom: 16px;
}
</style>
