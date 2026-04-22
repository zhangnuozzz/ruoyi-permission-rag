<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="策略编码" prop="policyCode">
        <el-input
          v-model="queryParams.policyCode"
          placeholder="请输入策略编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="策略名称" prop="policyName">
        <el-input
          v-model="queryParams.policyName"
          placeholder="请输入策略名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="优先级，数值越小优先级越高" prop="priority">
        <el-input
          v-model="queryParams.priority"
          placeholder="请输入优先级，数值越小优先级越高"
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
          v-hasPermi="['system:policy:add']"
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
          v-hasPermi="['system:policy:edit']"
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
          v-hasPermi="['system:policy:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:policy:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="policyList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="id" />
      <el-table-column label="策略编码" align="center" prop="policyCode" />
      <el-table-column label="策略名称" align="center" prop="policyName" />
      <el-table-column label="策略效果" align="center" prop="effect" />
      <el-table-column label="主体类型" align="center" prop="subjectType" />
      <el-table-column label="主体条件表达式" align="center" prop="subjectExpr" />
      <el-table-column label="资源条件表达式" align="center" prop="resourceExpr" />
      <el-table-column label="环境条件表达式" align="center" prop="envExpr" />
      <el-table-column label="优先级，数值越小优先级越高" align="center" prop="priority" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:policy:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:policy:remove']"
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

    <!-- 添加或修改策略管理对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="策略编码" prop="policyCode">
          <el-input v-model="form.policyCode" placeholder="请输入策略编码" />
        </el-form-item>
        <el-form-item label="策略名称" prop="policyName">
          <el-input v-model="form.policyName" placeholder="请输入策略名称" />
        </el-form-item>
        <el-form-item label="主体条件表达式" prop="subjectExpr">
          <el-input v-model="form.subjectExpr" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="资源条件表达式" prop="resourceExpr">
          <el-input v-model="form.resourceExpr" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="环境条件表达式" prop="envExpr">
          <el-input v-model="form.envExpr" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="优先级，数值越小优先级越高" prop="priority">
          <el-input v-model="form.priority" placeholder="请输入优先级，数值越小优先级越高" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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
import { listPolicy, getPolicy, delPolicy, addPolicy, updatePolicy } from "@/api/system/policy"

export default {
  name: "Policy",
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
      // 策略管理表格数据
      policyList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        policyCode: null,
        policyName: null,
        effect: null,
        subjectType: null,
        priority: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        policyCode: [
          { required: true, message: "策略编码不能为空", trigger: "blur" }
        ],
        policyName: [
          { required: true, message: "策略名称不能为空", trigger: "blur" }
        ],
        effect: [
          { required: true, message: "策略效果不能为空", trigger: "change" }
        ],
        subjectType: [
          { required: true, message: "主体类型不能为空", trigger: "change" }
        ],
        priority: [
          { required: true, message: "优先级，数值越小优先级越高不能为空", trigger: "blur" }
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
    /** 查询策略管理列表 */
    getList() {
      this.loading = true
      listPolicy(this.queryParams).then(response => {
        this.policyList = response.rows
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
        policyCode: null,
        policyName: null,
        effect: null,
        subjectType: null,
        subjectExpr: null,
        resourceExpr: null,
        envExpr: null,
        priority: null,
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
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加策略管理"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getPolicy(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改策略管理"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updatePolicy(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addPolicy(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除策略管理编号为"' + ids + '"的数据项？').then(function() {
        return delPolicy(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/policy/export', {
        ...this.queryParams
      }, `policy_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
