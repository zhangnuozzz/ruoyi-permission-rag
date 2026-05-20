<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="文档ID" prop="docId">
        <el-input
          v-model="queryParams.docId"
          placeholder="请输入文档ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文档名称" prop="docName">
        <el-input
          v-model="queryParams.docName"
          placeholder="请输入文档名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="知悉范围编码" prop="scopeCode">
        <el-input
          v-model="queryParams.scopeCode"
          placeholder="请输入知悉范围编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文档密级" prop="securityLevel">
        <el-input
          v-model="queryParams.securityLevel"
          placeholder="请输入文档密级"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属用户组编码" prop="ownerGroupCode">
        <el-input
          v-model="queryParams.ownerGroupCode"
          placeholder="请输入所属用户组编码"
          clearable
          @keyup.enter.native="handleQuery"
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
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:ragDoc:add']"
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
          v-hasPermi="['system:ragDoc:edit']"
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
          v-hasPermi="['system:ragDoc:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:ragDoc:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <div class="rag-doc-header">
      <div>
        <div class="rag-doc-title">文档权限标签</div>
        <div class="rag-doc-desc">
          用于维护 RAG 文档的知悉范围、密级与所属用户组。通过“RAG 文件入库”上传成功的文件，会自动回写到本表，为后续权限过滤和安全检索提供基础数据。
        </div>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="ragDocList"
      border
      stripe
      class="rag-doc-table"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" align="center" fixed="left" />
      <el-table-column label="主键ID" align="center" prop="id" width="80" fixed="left" />

      <el-table-column label="文档ID" align="center" prop="docId" width="230" show-overflow-tooltip>
        <template slot-scope="scope">
          <span class="mono-text">{{ scope.row.docId }}</span>
        </template>
      </el-table-column>

      <el-table-column label="文档名称" align="center" prop="docName" min-width="180" show-overflow-tooltip />

      <el-table-column label="知悉范围" align="center" prop="scopeCode" width="140" show-overflow-tooltip>
        <template slot-scope="scope">
          <el-tag size="mini" type="info">{{ scope.row.scopeCode }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="文档密级" align="center" prop="securityLevel" width="120">
        <template slot-scope="scope">
          <el-tag
            size="mini"
            :type="scope.row.securityLevel === 'SECRET' ? 'danger' : scope.row.securityLevel === 'INTERNAL' ? 'warning' : 'success'"
          >
            {{ scope.row.securityLevel }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="所属用户组" align="center" prop="ownerGroupCode" width="160" show-overflow-tooltip />

      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === '0' ? 'success' : 'danger'">
            {{ scope.row.status === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="备注 / 来源说明" align="left" prop="remark" min-width="320" show-overflow-tooltip />

      <el-table-column label="创建时间" align="center" prop="createTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>

      <el-table-column label="更新时间" align="center" prop="updateTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" align="center" width="140" fixed="right" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:ragDoc:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:ragDoc:remove']"
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

    <!-- 添加或修改文档权限标签对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="文档ID" prop="docId">
              <el-input v-model="form.docId" placeholder="请输入文档ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="文档名称" prop="docName">
              <el-input v-model="form.docName" placeholder="请输入文档名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="知悉范围编码" prop="scopeCode">
              <el-input v-model="form.scopeCode" placeholder="请输入知悉范围编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="文档密级" prop="securityLevel">
              <el-input v-model="form.securityLevel" placeholder="请输入文档密级" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所属用户组编码" prop="ownerGroupCode">
              <el-input v-model="form.ownerGroupCode" placeholder="请输入所属用户组编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listRagDoc, getRagDoc, delRagDoc, addRagDoc, updateRagDoc } from "@/api/system/ragDoc"

export default {
  name: "RagDoc",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 文档权限标签表格数据
      ragDocList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        docId: null,
        docName: null,
        scopeCode: null,
        securityLevel: null,
        ownerGroupCode: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        docId: [
          { required: true, message: "文档ID不能为空", trigger: "blur" }
        ],
        docName: [
          { required: true, message: "文档名称不能为空", trigger: "blur" }
        ],
        scopeCode: [
          { required: true, message: "知悉范围编码不能为空", trigger: "blur" }
        ],
        securityLevel: [
          { required: true, message: "文档密级不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "状态不能为空", trigger: "change" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询文档权限标签列表 */
    getList() {
      this.loading = true
      listRagDoc(this.queryParams).then(response => {
        this.ragDocList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        docId: null,
        docName: null,
        scopeCode: null,
        securityLevel: null,
        ownerGroupCode: null,
        status: null,
        remark: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        delFlag: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加文档权限标签"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getRagDoc(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改文档权限标签"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateRagDoc(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addRagDoc(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除文档权限标签编号为"' + ids + '"的数据项？').then(function() {
        return delRagDoc(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/ragDoc/export', {
        ...this.queryParams
      }, `ragDoc_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.rag-doc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.rag-doc-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.rag-doc-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}

.rag-doc-table {
  width: 100%;
}

.mono-text {
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 12px;
}
</style>

