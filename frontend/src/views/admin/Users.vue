<script setup>
import { ref, onMounted, watch } from 'vue'
import { getUsers, createUser, updateUser, resetPassword, deleteUser, getTeachers } from '../../api'
import Icon from '../../components/Icon.vue'
import Segmented from '../../components/Segmented.vue'
import EmptyState from '../../components/EmptyState.vue'
import LoadingBlock from '../../components/LoadingBlock.vue'
import Pagination from '../../components/Pagination.vue'
import Modal from '../../components/Modal.vue'
import { ROLE_MAP } from '../../utils/constants'
import { toast } from '../../utils/toast'

const ROLE_FILTERS = [
  { value: '', label: '全部' },
  { value: 'STUDENT', label: '学生' },
  { value: 'TEACHER', label: '辅导员' },
  { value: 'ADMIN', label: '管理员' }
]

const roleFilter = ref('')
const keyword = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const list = ref([])
const loading = ref(false)
const teachers = ref([])

async function load() {
  loading.value = true
  try {
    const data = await getUsers({
      role: roleFilter.value || undefined,
      keyword: keyword.value.trim() || undefined,
      page: page.value, size
    })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) { list.value = [] } finally { loading.value = false }
}

async function loadTeachers() {
  try { teachers.value = (await getTeachers()) || [] } catch (e) {}
}

watch(roleFilter, () => { page.value = 1; load() })
onMounted(() => { load(); loadTeachers() })
function search() { page.value = 1; load() }
function changePage(p) { page.value = p; load() }

// ---- 新增/编辑弹窗 ----
const showEdit = ref(false)
const editing = ref(null) // null=新增
const form = ref({})
const saving = ref(false)

function openCreate() {
  editing.value = null
  form.value = { username: '', password: '', realName: '', role: 'STUDENT', studentNo: '', className: '', phone: '', teacherId: '' }
  showEdit.value = true
}
function openEdit(row) {
  editing.value = row
  form.value = {
    username: row.username, realName: row.realName, role: row.role,
    studentNo: row.studentNo || '', className: row.className || '',
    phone: row.phone || '', teacherId: row.teacherId || ''
  }
  showEdit.value = true
}

async function save() {
  const f = form.value
  if (!f.username?.trim() || !f.realName?.trim()) { toast.warning('用户名和姓名必填'); return }
  if (!editing.value && !f.password) { toast.warning('新增用户必须设置密码'); return }
  if (f.role === 'STUDENT' && !f.teacherId) { toast.warning('学生必须绑定辅导员'); return }
  saving.value = true
  try {
    const payload = {
      username: f.username.trim(),
      realName: f.realName.trim(),
      role: f.role,
      studentNo: f.role === 'STUDENT' ? f.studentNo?.trim() || undefined : undefined,
      className: f.role === 'STUDENT' ? f.className?.trim() || undefined : undefined,
      phone: f.phone?.trim() || undefined,
      teacherId: f.role === 'STUDENT' ? f.teacherId : undefined
    }
    if (editing.value) {
      await updateUser(editing.value.id, payload)
      toast.success('已保存')
    } else {
      await createUser({ ...payload, password: f.password })
      toast.success('用户已创建')
    }
    showEdit.value = false
    load()
    if (form.value.role === 'TEACHER') loadTeachers()
  } catch (e) {} finally { saving.value = false }
}

// ---- 重置密码 ----
const pwdTarget = ref(null)
const newPwd = ref('')
const pwdSaving = ref(false)
function openPwd(row) { pwdTarget.value = row; newPwd.value = '' }
async function doResetPwd() {
  if (!newPwd.value || newPwd.value.length < 6) { toast.warning('密码至少 6 位'); return }
  pwdSaving.value = true
  try {
    await resetPassword(pwdTarget.value.id, newPwd.value)
    toast.success('密码已重置')
    pwdTarget.value = null
  } catch (e) {} finally { pwdSaving.value = false }
}

// ---- 删除 ----
const delTarget = ref(null)
const deleting = ref(false)
async function doDelete() {
  deleting.value = true
  try {
    await deleteUser(delTarget.value.id)
    toast.success('用户已删除')
    delTarget.value = null
    load()
  } catch (e) {} finally { deleting.value = false }
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="large-title">用户管理</h1>
        <p class="page-subtitle">管理学生、辅导员与管理员账号</p>
      </div>
      <button class="btn btn-primary" type="button" @click="openCreate">
        <Icon name="plus" :size="16" />新增用户
      </button>
    </div>

    <div class="toolbar">
      <Segmented v-model="roleFilter" :options="ROLE_FILTERS" />
      <form class="search-wrap" @submit.prevent="search">
        <Icon name="search" :size="15" class="search-icon" />
        <input v-model="keyword" class="input search-input" placeholder="搜索用户名 / 姓名 / 学号" @keyup.enter="search" />
      </form>
    </div>

    <div class="card">
      <LoadingBlock v-if="loading" />
      <EmptyState v-else-if="!list.length" icon="users" text="没有找到用户" />

      <template v-else>
        <table class="apple-table">
          <thead>
            <tr><th>用户名</th><th>姓名</th><th>角色</th><th>学号</th><th>班级</th><th>辅导员</th><th>电话</th><th style="text-align:right">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id">
              <td><b>{{ row.username }}</b></td>
              <td>{{ row.realName }}</td>
              <td><span class="pill" :class="ROLE_MAP[row.role]?.pill || 'pill-gray'">{{ ROLE_MAP[row.role]?.text || row.role }}</span></td>
              <td>{{ row.studentNo || '-' }}</td>
              <td>{{ row.className || '-' }}</td>
              <td>{{ row.teacherName || '-' }}</td>
              <td>{{ row.phone || '-' }}</td>
              <td>
                <div class="row-ops">
                  <button class="op-btn" type="button" title="编辑" @click="openEdit(row)"><Icon name="edit" :size="15" /></button>
                  <button class="op-btn" type="button" title="重置密码" @click="openPwd(row)"><Icon name="key" :size="15" /></button>
                  <button class="op-btn danger" type="button" title="删除" @click="delTarget = row"><Icon name="trash" :size="15" /></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <Pagination :total="total" :page="page" :size="size" @change="changePage" />
      </template>
    </div>

    <!-- 新增/编辑 -->
    <Modal :show="showEdit" :title="editing ? '编辑用户' : '新增用户'" width="480px" @close="showEdit = false">
      <div class="form-col">
        <div class="grid-2">
          <div class="field">
            <label class="field-label">用户名</label>
            <input v-model="form.username" class="input" placeholder="登录账号" :disabled="!!editing" />
          </div>
          <div class="field">
            <label class="field-label">姓名</label>
            <input v-model="form.realName" class="input" placeholder="真实姓名" />
          </div>
        </div>
        <div class="field" v-if="!editing">
          <label class="field-label">初始密码</label>
          <input v-model="form.password" type="password" class="input" placeholder="至少 6 位" />
        </div>
        <div class="field">
          <label class="field-label">角色</label>
          <div class="segmented">
            <button v-for="r in ['STUDENT', 'TEACHER', 'ADMIN']" :key="r" type="button"
              :class="{ active: form.role === r }" @click="form.role = r">{{ ROLE_MAP[r].text }}</button>
          </div>
        </div>
        <template v-if="form.role === 'STUDENT'">
          <div class="grid-2">
            <div class="field">
              <label class="field-label">学号</label>
              <input v-model="form.studentNo" class="input" placeholder="如 20230001" />
            </div>
            <div class="field">
              <label class="field-label">班级</label>
              <input v-model="form.className" class="input" placeholder="如 软件2101" />
            </div>
          </div>
          <div class="field">
            <label class="field-label">辅导员（必选）</label>
            <div class="select-wrap">
              <select v-model="form.teacherId" class="select">
                <option value="" disabled>选择辅导员</option>
                <option v-for="t in teachers" :key="t.id" :value="t.id">{{ t.realName }}</option>
              </select>
              <Icon name="chevron-down" :size="14" class="chev" />
            </div>
          </div>
        </template>
        <div class="field">
          <label class="field-label">电话（选填）</label>
          <input v-model="form.phone" class="input" placeholder="手机号" />
        </div>
      </div>
      <template #footer>
        <button class="btn btn-secondary" type="button" @click="showEdit = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
      </template>
    </Modal>

    <!-- 重置密码 -->
    <Modal :show="!!pwdTarget" title="重置密码" width="380px" @close="pwdTarget = null">
      <p v-if="pwdTarget" style="font-size:13.5px;color:var(--text-2);margin-bottom:12px">
        为 <b style="color:var(--text-1)">{{ pwdTarget.realName }}（{{ pwdTarget.username }}）</b> 设置新密码：
      </p>
      <div class="field">
        <input v-model="newPwd" type="password" class="input" placeholder="新密码，至少 6 位" />
      </div>
      <template #footer>
        <button class="btn btn-secondary" type="button" @click="pwdTarget = null">取消</button>
        <button class="btn btn-primary" type="button" :disabled="pwdSaving" @click="doResetPwd">确认重置</button>
      </template>
    </Modal>

    <!-- 删除确认 -->
    <Modal :show="!!delTarget" title="删除用户" width="380px" @close="delTarget = null">
      <p v-if="delTarget" style="font-size:14px;color:var(--text-2);line-height:1.7">
        确定删除用户 <b style="color:var(--text-1)">{{ delTarget.realName }}（{{ delTarget.username }}）</b>？<br>
        删除后该账号将无法登录（逻辑删除）。
      </p>
      <template #footer>
        <button class="btn btn-secondary" type="button" @click="delTarget = null">取消</button>
        <button class="btn btn-danger" type="button" :disabled="deleting" @click="doDelete">确认删除</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.toolbar {
  display: flex; align-items: center; justify-content: space-between;
  gap: 14px; margin-bottom: 16px; flex-wrap: wrap;
}
.search-wrap { position: relative; width: 280px; }
.search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--text-2); }
.search-input { padding-left: 34px; border-radius: var(--radius-pill); }

.row-ops { display: flex; gap: 6px; justify-content: flex-end; }
.op-btn {
  width: 29px; height: 29px;
  border-radius: 8px;
  border: none;
  background: rgba(0, 0, 0, .05);
  color: var(--text-2);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all .15s ease-out;
}
.op-btn:hover { background: var(--accent-soft); color: var(--accent); }
.op-btn.danger:hover { background: var(--red-soft); color: var(--red); }

.form-col { display: flex; flex-direction: column; gap: 14px; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
</style>
