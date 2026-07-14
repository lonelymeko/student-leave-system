<script setup>
// 管理员 — 用户管理：角色筛选 + 搜索 + 列表；新增/编辑/重置密码/删除（SheetModal）
import { ref, watch, computed } from 'vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { getUsers, createUser, updateUser, resetPassword, deleteUser, getTeachers } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import EmptyBox from '../../components/EmptyBox.vue'
import TabBar from '../../components/TabBar.vue'
import SheetModal from '../../components/SheetModal.vue'
import { ROLE_MAP } from '../../utils/constants'
import { getToken, getUser } from '../../utils/auth'

const ROLE_FILTERS = [
  { value: '', label: '全部' },
  { value: 'STUDENT', label: '学生' },
  { value: 'TEACHER', label: '辅导员' },
  { value: 'ADMIN', label: '管理员' }
]
const ROLE_OPTIONS = ['STUDENT', 'TEACHER', 'ADMIN']

const roleFilter = ref('')
const keyword = ref('')
const page = ref(1)
const size = 10
const total = ref(0)
const list = ref([])
const loading = ref(false)
const finished = ref(false)
const teachers = ref([])

async function load(reset = false) {
  if (loading.value) return
  if (reset) { page.value = 1; finished.value = false }
  loading.value = true
  try {
    const data = await getUsers({
      role: roleFilter.value || undefined,
      keyword: keyword.value.trim() || undefined,
      page: page.value, size
    })
    const records = data?.records || []
    list.value = reset ? records : [...list.value, ...records]
    total.value = data?.total || 0
    finished.value = list.value.length >= total.value
  } catch (e) {
    if (reset) list.value = []
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

async function loadTeachers() {
  try { teachers.value = (await getTeachers()) || [] } catch (e) {}
}

watch(roleFilter, () => load(true))

onShow(() => {
  if (!getToken()) { uni.reLaunch({ url: '/pages/login/login' }); return }
  if (getUser()?.role !== 'ADMIN') { uni.reLaunch({ url: '/pages/login/login' }); return }
  load(true)
  loadTeachers()
})
onPullDownRefresh(() => load(true))
onReachBottom(() => {
  if (!finished.value && !loading.value) { page.value += 1; load() }
})

function search() { load(true) }
const roleInfo = r => ROLE_MAP[r] || { text: r, pill: 'pill-gray' }

// ---- 新增/编辑弹层 ----
const showEdit = ref(false)
const editing = ref(null) // null=新增
const form = ref({})
const saving = ref(false)

const teacherIndex = computed({
  get() {
    const i = teachers.value.findIndex(t => t.id === form.value.teacherId)
    return i < 0 ? -1 : i
  },
  set(i) { form.value.teacherId = teachers.value[i]?.id || '' }
})
const teacherLabel = computed(() => {
  const t = teachers.value.find(t => t.id === form.value.teacherId)
  return t ? t.realName : ''
})

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
  if (!f.username?.trim() || !f.realName?.trim()) { uni.showToast({ title: '用户名和姓名必填', icon: 'none' }); return }
  if (!editing.value && (!f.password || f.password.length < 6)) { uni.showToast({ title: '新增用户密码至少 6 位', icon: 'none' }); return }
  if (f.role === 'STUDENT' && !f.teacherId) { uni.showToast({ title: '学生必须绑定辅导员', icon: 'none' }); return }
  if (saving.value) return
  saving.value = true
  try {
    const payload = {
      username: f.username.trim(),
      realName: f.realName.trim(),
      role: f.role,
      studentNo: f.role === 'STUDENT' ? (f.studentNo?.trim() || undefined) : undefined,
      className: f.role === 'STUDENT' ? (f.className?.trim() || undefined) : undefined,
      phone: f.phone?.trim() || undefined,
      teacherId: f.role === 'STUDENT' ? f.teacherId : undefined
    }
    if (editing.value) {
      await updateUser(editing.value.id, payload)
      uni.showToast({ title: '已保存', icon: 'none' })
    } else {
      await createUser({ ...payload, password: f.password })
      uni.showToast({ title: '用户已创建', icon: 'none' })
    }
    showEdit.value = false
    load(true)
    if (f.role === 'TEACHER') loadTeachers()
  } catch (e) {} finally { saving.value = false }
}

// ---- 重置密码 ----
const pwdTarget = ref(null)
const newPwd = ref('')
const pwdSaving = ref(false)
function openPwd(row) { pwdTarget.value = row; newPwd.value = '' }
async function doResetPwd() {
  if (!newPwd.value || newPwd.value.length < 6) { uni.showToast({ title: '密码至少 6 位', icon: 'none' }); return }
  if (pwdSaving.value) return
  pwdSaving.value = true
  try {
    await resetPassword(pwdTarget.value.id, newPwd.value)
    uni.showToast({ title: '密码已重置', icon: 'none' })
    pwdTarget.value = null
  } catch (e) {} finally { pwdSaving.value = false }
}

// ---- 删除 ----
const delTarget = ref(null)
const deleting = ref(false)
async function doDelete() {
  if (deleting.value) return
  deleting.value = true
  try {
    await deleteUser(delTarget.value.id)
    uni.showToast({ title: '用户已删除', icon: 'none' })
    delTarget.value = null
    load(true)
  } catch (e) {} finally { deleting.value = false }
}
</script>

<template>
  <view class="page-wrap">
    <view class="page-head">
      <view>
        <view class="large-title">用户管理</view>
        <view class="page-subtitle">学生 / 辅导员 / 管理员账号</view>
      </view>
      <view class="btn btn-primary btn-sm" @click="openCreate">
        <AppIcon name="plus" :size="14" color="#ffffff" />
        <text>新增</text>
      </view>
    </view>

    <view style="margin-bottom: 12px">
      <view class="segmented" style="display: flex">
        <view
          v-for="r in ROLE_FILTERS" :key="r.value"
          class="seg-item" style="flex: 1; text-align: center"
          :class="{ active: roleFilter === r.value }"
          @click="roleFilter = r.value"
        >{{ r.label }}</view>
      </view>
    </view>

    <view class="search-wrap" style="margin-bottom: 16px">
      <view class="search-icon"><AppIcon name="search" :size="15" color="#8e8e93" /></view>
      <input
        v-model="keyword"
        class="input search-input"
        placeholder="搜索用户名 / 姓名 / 学号"
        placeholder-class="ph"
        confirm-type="search"
        @confirm="search"
      />
    </view>

    <view class="card">
      <view v-if="loading && !list.length" class="loading-block">
        <view class="spinner" /><text>加载中…</text>
      </view>
      <EmptyBox v-else-if="!list.length" icon="users" text="没有找到用户" />

      <template v-else>
        <view v-for="row in list" :key="row.id" class="group-row user-row">
          <view class="row-main">
            <view class="row-line1">
              <text class="row-name">{{ row.realName }}</text>
              <view class="pill" :class="roleInfo(row.role).pill">{{ roleInfo(row.role).text }}</view>
            </view>
            <view class="row-sub">
              <text>账号 {{ row.username }}</text>
              <text v-if="row.role === 'STUDENT'"> · {{ row.studentNo || '-' }} · {{ row.className || '-' }}</text>
              <text v-if="row.role === 'STUDENT' && row.teacherName"> · 辅导员 {{ row.teacherName }}</text>
              <text v-if="row.phone"> · {{ row.phone }}</text>
            </view>
          </view>
          <view class="row-ops">
            <view class="op-btn" @click="openEdit(row)"><AppIcon name="edit" :size="15" color="#6e6e73" /></view>
            <view class="op-btn" @click="openPwd(row)"><AppIcon name="key" :size="15" color="#6e6e73" /></view>
            <view class="op-btn danger" @click="delTarget = row"><AppIcon name="trash" :size="15" color="#ff3b30" /></view>
          </view>
        </view>

        <view v-if="list.length && finished" class="list-end">— 共 {{ total }} 人 —</view>
        <view v-else-if="loading" class="list-end">加载中…</view>
      </template>
    </view>

    <!-- 新增/编辑 -->
    <SheetModal :show="showEdit" :title="editing ? '编辑用户' : '新增用户'" @close="showEdit = false">
      <view class="form-col">
        <view class="field">
          <text class="field-label">用户名</text>
          <input v-model="form.username" class="input" placeholder="登录账号" placeholder-class="ph" :disabled="!!editing" />
        </view>
        <view class="field">
          <text class="field-label">姓名</text>
          <input v-model="form.realName" class="input" placeholder="真实姓名" placeholder-class="ph" />
        </view>
        <view class="field" v-if="!editing">
          <text class="field-label">初始密码（至少 6 位）</text>
          <input v-model="form.password" class="input" password placeholder="设置初始密码" placeholder-class="ph" />
        </view>
        <view class="field">
          <text class="field-label">角色</text>
          <view class="segmented" style="align-self: flex-start">
            <view
              v-for="r in ROLE_OPTIONS" :key="r"
              class="seg-item" :class="{ active: form.role === r }"
              @click="form.role = r"
            >{{ roleInfo(r).text }}</view>
          </view>
        </view>
        <template v-if="form.role === 'STUDENT'">
          <view class="field">
            <text class="field-label">学号</text>
            <input v-model="form.studentNo" class="input" placeholder="如 20230001" placeholder-class="ph" />
          </view>
          <view class="field">
            <text class="field-label">班级</text>
            <input v-model="form.className" class="input" placeholder="如 软件2101" placeholder-class="ph" />
          </view>
          <view class="field">
            <text class="field-label">辅导员（必选）</text>
            <picker :range="teachers" range-key="realName" :value="teacherIndex" @change="e => teacherIndex = Number(e.detail.value)">
              <view class="picker-box">
                <text class="val" :class="{ empty: !teacherLabel }">{{ teacherLabel || '选择辅导员' }}</text>
                <AppIcon name="chevron-right" :size="15" color="#6e6e73" />
              </view>
            </picker>
          </view>
        </template>
        <view class="field">
          <text class="field-label">电话（选填）</text>
          <input v-model="form.phone" class="input" type="number" placeholder="手机号" placeholder-class="ph" />
        </view>
      </view>
      <template #footer>
        <view class="btn btn-secondary" @click="showEdit = false">取消</view>
        <view class="btn btn-primary" :class="{ disabled: saving }" @click="save">{{ saving ? '保存中…' : '保存' }}</view>
      </template>
    </SheetModal>

    <!-- 重置密码 -->
    <SheetModal :show="!!pwdTarget" title="重置密码" @close="pwdTarget = null">
      <view v-if="pwdTarget" class="confirm-text" style="margin-bottom: 14px">
        为 <text style="color: #1d1d1f; font-weight: 600">{{ pwdTarget.realName }}（{{ pwdTarget.username }}）</text> 设置新密码：
      </view>
      <view class="field">
        <input v-model="newPwd" class="input" password placeholder="新密码，至少 6 位" placeholder-class="ph" />
      </view>
      <template #footer>
        <view class="btn btn-secondary" @click="pwdTarget = null">取消</view>
        <view class="btn btn-primary" :class="{ disabled: pwdSaving }" @click="doResetPwd">确认重置</view>
      </template>
    </SheetModal>

    <!-- 删除确认 -->
    <SheetModal :show="!!delTarget" title="删除用户" @close="delTarget = null">
      <view v-if="delTarget" class="confirm-text">
        确定删除用户 <text style="color: #1d1d1f; font-weight: 600">{{ delTarget.realName }}（{{ delTarget.username }}）</text>？{{ '\n' }}删除后该账号将无法登录（逻辑删除）。
      </view>
      <template #footer>
        <view class="btn btn-secondary" @click="delTarget = null">取消</view>
        <view class="btn btn-danger" :class="{ disabled: deleting }" @click="doDelete">确认删除</view>
      </template>
    </SheetModal>

    <TabBar current="admin-users" />
  </view>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.search-wrap { position: relative; }
.search-icon { position: absolute; left: 13px; top: 50%; transform: translateY(-50%); z-index: 2; display: flex; align-items: center; }
.search-input { padding-left: 38px; border-radius: var(--radius-pill); }

.user-row { gap: 10px; align-items: center; }
.row-main { flex: 1; min-width: 0; }
.row-line1 { display: flex; align-items: center; gap: 8px; margin-bottom: 3px; }
.row-name { font-size: 15px; font-weight: 600; }
.row-sub {
  font-size: 12.5px; color: var(--text-2);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.row-ops { display: flex; gap: 6px; flex-shrink: 0; }
.op-btn {
  width: 32px; height: 32px; border-radius: 9px;
  background: rgba(0, 0, 0, .05);
  display: flex; align-items: center; justify-content: center;
}
.op-btn:active { background: rgba(0, 0, 0, .09); }
.op-btn.danger { background: var(--red-soft); }

.list-end { text-align: center; font-size: 12px; color: #a1a1a6; padding: 14px 0 16px; }
.form-col { display: flex; flex-direction: column; gap: 14px; }
.confirm-text { color: var(--text-2); font-size: 14px; line-height: 1.7; white-space: pre-line; }
</style>
