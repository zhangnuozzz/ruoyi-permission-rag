<template>
  <div class="app-container user-security-attr-page">
    <el-card class="intro-card" shadow="never">
      <div class="intro-title">用户安全属性管理</div>
      <div class="intro-desc">
        用于维护 VACP 零信任安全向量检索中的用户密级、访问状态、访问时间窗口和风险等级。
        后续 ABAC、TBAC、BBAC、访问决策引擎、风险评分和安全检索过滤均基于这些属性完成。
      </div>
    </el-card>

    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      v-show="showSearch"
      label-width="88px"
      class="query-form"
    >
      <el-form-item label="用户名" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入用户名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item label="用户昵称" prop="nickName">
        <el-input
          v-model="queryParams.nickName"
          placeholder="请输入用户昵称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item label="用户密级" prop="secretLevel">
        <el-select v-model="queryParams.secretLevel" placeholder="全部" clearable>
          <el-option label="公开" value="PUBLIC" />
          <el-option label="内部" value="INTERNAL" />
          <el-option label="秘密" value="SECRET" />
          <el-option label="机密" value="CONFIDENTIAL" />
        </el-select>
      </el-form-item>

      <el-form-item label="访问状态" prop="accessStatus">
        <el-select v-model="queryParams.accessStatus" placeholder="全部" clearable>
          <el-option label="启用" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
          <el-option label="锁定" value="LOCKED" />
        </el-select>
      </el-form-item>

      <el-form-item label="风险等级" prop="riskLevel">
        <el-select v-model="queryParams.riskLevel" placeholder="全部" clearable>
          <el-option label="低风险" value="LOW" />
          <el-option label="中风险" value="MEDIUM" />
          <el-option label="高风险" value="HIGH" />
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
          v-hasPermi="['system:userSecurityAttr:add']"
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
          v-hasPermi="['system:userSecurityAttr:edit']"
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
          v-hasPermi="['system:userSecurityAttr:remove']"
        >删除</el-button>
      </el-col>

      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:userSecurityAttr:export']"
        >导出</el-button>
      </el-col>

      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="userSecurityAttrList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />

      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="用户ID" align="center" prop="userId" width="90" />
      <el-table-column label="用户名" align="center" prop="userName" min-width="120" />
      <el-table-column label="用户昵称" align="center" prop="nickName" min-width="120" />

      <el-table-column label="用户密级" align="center" prop="secretLevel" width="110">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.secretLevel === 'PUBLIC'" type="success">公开</el-tag>
          <el-tag v-else-if="scope.row.secretLevel === 'INTERNAL'" type="info">内部</el-tag>
          <el-tag v-else-if="scope.row.secretLevel === 'SECRET'" type="warning">秘密</el-tag>
          <el-tag v-else-if="scope.row.secretLevel === 'CONFIDENTIAL'" type="danger">机密</el-tag>
          <el-tag v-else>{{ scope.row.secretLevel }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="访问状态" align="center" prop="accessStatus" width="110">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.accessStatus === 'ACTIVE'" type="success">启用</el-tag>
          <el-tag v-else-if="scope.row.accessStatus === 'DISABLED'" type="info">禁用</el-tag>
          <el-tag v-else-if="scope.row.accessStatus === 'LOCKED'" type="danger">锁定</el-tag>
          <el-tag v-else>{{ scope.row.accessStatus }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="访问时间窗口" align="center" min-width="180">
        <template slot-scope="scope">
          <span v-if="scope.row.accessStartTime || scope.row.accessEndTime">
            {{ scope.row.accessStartTime || '--' }} 至 {{ scope.row.accessEndTime || '--' }}
          </span>
          <span v-else>不限制</span>
        </template>
      </el-table-column>

      <el-table-column label="风险等级" align="center" prop="riskLevel" width="110">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.riskLevel === 'LOW'" type="success">低风险</el-tag>
          <el-tag v-else-if="scope.row.riskLevel === 'MEDIUM'" type="warning">中风险</el-tag>
          <el-tag v-else-if="scope.row.riskLevel === 'HIGH'" type="danger">高风险</el-tag>
          <el-tag v-else>{{ scope.row.riskLevel }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="失败次数" align="center" prop="failCount" width="100" />
      <el-table-column label="最近访问IP" align="center" prop="lastAccessIp" min-width="130" />
      <el-table-column label="最近访问时间" align="center" prop="lastAccessTime" min-width="160" />
      <el-table-column label="备注" align="center" prop="remark" min-width="160" show-overflow-tooltip />

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:userSecurityAttr:edit']"
          >修改</el-button>

          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:userSecurityAttr:remove']"
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

    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-alert
        title="说明：用户安全属性用于后续访问决策。用户密级、访问状态、访问时间窗口会参与权限判断。"
        type="info"
        :closable="false"
        show-icon
        class="dialog-alert"
      />

      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户ID" prop="userId">
              <el-input v-model="form.userId" placeholder="请输入用户ID" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="用户名" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="用户密级" prop="secretLevel">
              <el-select v-model="form.secretLevel" placeholder="请选择用户密级" style="width: 100%">
                <el-option label="公开" value="PUBLIC" />
                <el-option label="内部" value="INTERNAL" />
                <el-option label="秘密" value="SECRET" />
                <el-option label="机密" value="CONFIDENTIAL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="访问状态" prop="accessStatus">
              <el-select v-model="form.accessStatus" placeholder="请选择访问状态" style="width: 100%">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="禁用" value="DISABLED" />
                <el-option label="锁定" value="LOCKED" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="风险等级" prop="riskLevel">
              <el-select v-model="form.riskLevel" placeholder="请选择风险等级" style="width: 100%">
                <el-option label="低风险" value="LOW" />
                <el-option label="中风险" value="MEDIUM" />
                <el-option label="高风险" value="HIGH" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="访问开始时间" prop="accessStartTime">
              <el-time-picker
                v-model="form.accessStartTime"
                value-format="HH:mm:ss"
                format="HH:mm:ss"
                placeholder="开始时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="访问结束时间" prop="accessEndTime">
              <el-time-picker
                v-model="form.accessEndTime"
                value-format="HH:mm:ss"
                format="HH:mm:ss"
                placeholder="结束时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="失败次数" prop="failCount">
              <el-input-number v-model="form.failCount" :min="0" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="最近访问IP" prop="lastAccessIp">
              <el-input v-model="form.lastAccessIp" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="最近访问时间" prop="lastAccessTime">
          <el-date-picker
            clearable
            v-model="form.lastAccessTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="请选择最近访问时间"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
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
import {
  listUserSecurityAttr,
  getUserSecurityAttr,
  addUserSecurityAttr,
  updateUserSecurityAttr,
  delUserSecurityAttr,
  exportUserSecurityAttr
} from '@/api/system/userSecurityAttr'

export default {
  name: 'UserSecurityAttr',
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      userSecurityAttrList: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: null,
        nickName: null,
        secretLevel: null,
        accessStatus: null,
        riskLevel: null
      },
      form: {},
      rules: {
        userId: [
          { required: true, message: '用户ID不能为空', trigger: 'blur' }
        ],
        userName: [
          { required: true, message: '用户名不能为空', trigger: 'blur' }
        ],
        secretLevel: [
          { required: true, message: '用户密级不能为空', trigger: 'change' }
        ],
        accessStatus: [
          { required: true, message: '访问状态不能为空', trigger: 'change' }
        ],
        riskLevel: [
          { required: true, message: '风险等级不能为空', trigger: 'change' }
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
      listUserSecurityAttr(this.queryParams).then(response => {
        this.userSecurityAttrList = response.rows
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
        id: null,
        userId: null,
        userName: null,
        nickName: null,
        secretLevel: 'PUBLIC',
        accessStatus: 'ACTIVE',
        accessStartTime: null,
        accessEndTime: null,
        riskLevel: 'LOW',
        failCount: 0,
        lastAccessIp: null,
        lastAccessTime: null,
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
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增用户安全属性'
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getUserSecurityAttr(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改用户安全属性'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateUserSecurityAttr(this.form).then(response => {
              this.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addUserSecurityAttr(this.form).then(response => {
              this.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$confirm('是否确认删除用户安全属性编号为 "' + ids + '" 的数据项?', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function() {
        return delUserSecurityAttr(ids)
      }).then(() => {
        this.getList()
        this.msgSuccess('删除成功')
      })
    },
    handleExport() {
      const queryParams = this.queryParams
      this.$confirm('是否确认导出所有用户安全属性数据项?', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function() {
        return exportUserSecurityAttr(queryParams)
      }).then(response => {
        this.download(response.msg)
      })
    }
  }
}
</script>

<style scoped>
.intro-card {
  margin-bottom: 14px;
  border-left: 4px solid #409eff;
}

.intro-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.intro-desc {
  color: #606266;
  line-height: 1.7;
}

.query-form {
  margin-top: 10px;
}

.dialog-alert {
  margin-bottom: 18px;
}
</style>
