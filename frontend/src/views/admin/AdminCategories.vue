<template>
  <div class="admin-categories">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-title">
        <h1>分类管理</h1>
        <p>管理商品分类和排序</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showAddCategoryDialog">
          <el-icon><Plus /></el-icon>
          新增分类
        </el-button>
      </div>
    </div>

    <!-- 分类列表 -->
    <div class="categories-section">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>商品分类 (共 {{ categories.length }} 个)</span>
          </div>
        </template>

        <el-table
          v-loading="loading"
          :data="categories"
          row-key="id"
          style="width: 100%"
        >
          <el-table-column label="分类图标" width="100" align="center">
            <template #default="{ row }">
              <div class="category-icon">
                <el-icon v-if="row.id <= 4"><FolderOpened /></el-icon>
                <el-icon v-else-if="row.id <= 6"><Files /></el-icon>
                <el-icon v-else><Folder /></el-icon>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="分类名称" min-width="120">
            <template #default="{ row }">
              <span class="category-name">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="80" align="center">
            <template #default="{ row }">
              <el-input-number 
                v-model="row.sort" 
                :min="1" 
                :max="99" 
                size="small"
                @change="handleSortChange(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                size="small" 
                @click="editCategory(row)"
              >
                编辑
              </el-button>
              <el-button 
                v-if="row.id !== 1" 
                type="danger" 
                size="small" 
                @click="deleteCategory(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 新增/编辑分类对话框 -->
    <el-dialog
      v-model="categoryDialog"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="500px"
      @close="closeCategoryDialog"
    >
      <el-form
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryRules"
        label-width="80px"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input 
            v-model="categoryForm.name" 
            placeholder="请输入分类名称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number 
            v-model="categoryForm.sort" 
            :min="1" 
            :max="99" 
            placeholder="请输入排序值"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="categoryForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="categoryDialog = false">取消</el-button>
          <el-button type="primary" @click="saveCategory">
            {{ isEdit ? '保存' : '新增' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 删除确认对话框 -->
    <el-dialog
      v-model="deleteDialog"
      title="删除分类"
      width="400px"
    >
      <div class="delete-content">
        <el-icon class="warning-icon" color="#f56c6c">
          <Warning />
        </el-icon>
        <div class="warning-text">
          确定要删除分类 "<strong>{{ deleteCategoryName }}</strong>" 吗？
          <br>
          <span class="warning-desc">
            删除后该分类下的所有商品将被归为"其他闲置"分类
          </span>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialog = false">取消</el-button>
          <el-button type="danger" @click="confirmDelete">
            确认删除
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, FolderOpened, Files, Folder, Warning } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const categoryFormRef = ref()
const categoryDialog = ref(false)
const deleteDialog = ref(false)
const isEdit = ref(false)
const deleteCategoryId = ref(null)
const deleteCategoryName = ref('')
const categories = ref([])

// 分类表单
const categoryForm = reactive({
  id: null,
  name: '',
  sort: 1,
  status: 1
})

// 表单验证规则
const categoryRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 20, message: '分类名称长度为 2-20 个字符', trigger: 'blur' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'blur' },
    { type: 'number', message: '排序值必须为数字', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 加载分类列表
const loadCategories = async () => {
  try {
    loading.value = true
    const response = await adminApi.getAdminCategories()
    categories.value = response
  } catch (error) {
    console.error('加载分类列表失败:', error)
    ElMessage.error('加载分类列表失败')
  } finally {
    loading.value = false
  }
}

// 显示新增分类对话框
const showAddCategoryDialog = () => {
  isEdit.value = false
  Object.keys(categoryForm).forEach(key => {
    categoryForm[key] = key === 'id' ? null : (key === 'sort' ? 1 : (key === 'status' ? 1 : ''))
  })
  categoryDialog.value = true
}

// 编辑分类
const editCategory = (category) => {
  isEdit.value = true
  Object.assign(categoryForm, category)
  categoryDialog.value = true
}

// 保存分类
const saveCategory = async () => {
  try {
    await categoryFormRef.value.validate()
    
    loading.value = true
    
    if (isEdit.value) {
      // 编辑分类
      await adminApi.updateCategory(categoryForm.id, categoryForm)
      ElMessage.success('分类更新成功')
    } else {
      // 新增分类
      await adminApi.addCategory(categoryForm)
      ElMessage.success('分类添加成功')
    }
    
    categoryDialog.value = false
    loadCategories()
  } catch (error) {
    console.error('保存分类失败:', error)
    ElMessage.error(isEdit.value ? '更新分类失败' : '添加分类失败')
  } finally {
    loading.value = false
  }
}

// 关闭分类对话框
const closeCategoryDialog = () => {
  categoryFormRef.value?.resetFields()
  categoryDialog.value = false
}

// 处理排序变更
const handleSortChange = (category) => {
  // 更新排序
  const updatedCategories = categories.value.map(cat => {
    if (cat.id === category.id) {
      return { ...cat, sort: category.sort }
    }
    return cat
  })
  
  // 重新排序
  updatedCategories.sort((a, b) => a.sort - b.sort)
  categories.value = updatedCategories
  
  ElMessage.success('排序更新成功')
}

// 删除分类
const deleteCategory = (category) => {
  deleteCategoryId.value = category.id
  deleteCategoryName.value = category.name
  deleteDialog.value = true
}

// 确认删除
const confirmDelete = async () => {
  try {
    loading.value = true
    await adminApi.deleteCategory(deleteCategoryId.value)
    ElMessage.success('分类删除成功')
    deleteDialog.value = false
    loadCategories()
  } catch (error) {
    console.error('删除分类失败:', error)
    ElMessage.error('删除分类失败')
  } finally {
    loading.value = false
  }
}

// 格式化日期时间
const formatDateTime = (timeStr) => {
  return formatDate(timeStr, 'YYYY-MM-DD HH:mm:ss')
}

// 初始化
onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.admin-categories {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-title h1 {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.header-title p {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.categories-section {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 18px;
}

.category-name {
  font-weight: 500;
  color: #303133;
}

.delete-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
}

.warning-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.warning-text {
  flex: 1;
  color: #606266;
  line-height: 1.6;
}

.warning-text strong {
  color: #f56c6c;
}

.warning-desc {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-top: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>