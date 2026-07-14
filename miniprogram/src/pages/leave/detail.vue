<script setup>
// 请假详情：基本信息 + 审批时间线；学生可撤回（PENDING）/ 申请销假（APPROVED）
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getLeaveDetail, revokeLeave, cancelApply, getLeaveAttachments } from '../../api'
import { BASE_URL, assetUrl } from '../../utils/request'
import AppIcon from '../../components/AppIcon.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyBox from '../../components/EmptyBox.vue'
import SheetModal from '../../components/SheetModal.vue'
import { typeText, fmtTime, ACTION_MAP } from '../../utils/constants'
import { getUser, getToken } from '../../utils/auth'

const detail = ref(null)
const loading = ref(true)
const failed = ref(false)
const me = getUser()
const role = me?.role || ''
let leaveId = null

// 附件
const attachments = ref([])
const uploading = ref(false)

// 是否本人（仅学生本人可上传证明）：用学号比对，学号是响应里唯一可用的身份标识
const isOwner = computed(() =>
  role === 'STUDENT' && !!me?.studentNo && detail.value?.studentNo === me.studentNo
)

async function load() {
  loading.value = true
  failed.value = false
  try {
    detail.value = await getLeaveDetail(leaveId)
    loadAttachments()
  } catch (e) {
    failed.value = true
  } finally {
    loading.value = false
  }
}

async function loadAttachments() {
  try {
    attachments.value = await getLeaveAttachments(leaveId) || []
  } catch (e) { attachments.value = [] }
}

const isImage = a => /\.(png|jpe?g|gif|bmp|webp)$/i.test(a.fileName || a.fileUrl || '')

function previewAttachment(a) {
  const url = assetUrl(a.fileUrl)
  if (isImage(a)) {
    const urls = attachments.value.filter(isImage).map(x => assetUrl(x.fileUrl))
    uni.previewImage({ current: url, urls })
  } else {
    uni.showToast({ title: '暂不支持预览该类型', icon: 'none' })
  }
}

function chooseAndUpload() {
  if (uploading.value) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: res => {
      const filePath = res.tempFilePaths?.[0]
      if (!filePath) return
      doUpload(filePath)
    }
  })
}

function doUpload(filePath) {
  uploading.value = true
  const token = uni.getStorageSync('token') || ''
  uni.uploadFile({
    url: `${BASE_URL}/leave/${leaveId}/attachment`,
    filePath,
    name: 'file',
    header: token ? { Authorization: `Bearer ${token}` } : {},
    success: res => {
      let body = {}
      try { body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data } catch (e) {}
      if (res.statusCode === 200 && body && body.code === 0) {
        uni.showToast({ title: '上传成功', icon: 'none' })
        loadAttachments()
      } else {
        uni.showToast({ title: (body && body.msg) || `上传失败 (${res.statusCode})`, icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '上传失败，请检查网络', icon: 'none' }),
    complete: () => { uploading.value = false }
  })
}

onLoad(query => {
  if (!getToken()) { uni.reLaunch({ url: '/pages/login/login' }); return }
  leaveId = query.id
  load()
})

const actionInfo = a => ACTION_MAP[a] || { text: a, color: '#6e6e73' }

function back() {
  uni.navigateBack({
    fail: () => uni.reLaunch({ url: role === 'TEACHER' ? '/pages/teacher/pending' : '/pages/student/leaves' })
  })
}

// ---- 撤回 ----
const showRevoke = ref(false)
const revoking = ref(false)
async function doRevoke() {
  if (revoking.value) return
  revoking.value = true
  try {
    await revokeLeave(detail.value.id)
    uni.showToast({ title: '已撤回', icon: 'none' })
    showRevoke.value = false
    load()
  } catch (e) {} finally { revoking.value = false }
}

// ---- 销假申请 ----
const showCancel = ref(false)
const cancelNote = ref('')
const canceling = ref(false)
async function doCancelApply() {
  if (canceling.value) return
  canceling.value = true
  try {
    await cancelApply(detail.value.id, { note: cancelNote.value.trim() })
    uni.showToast({ title: '销假申请已提交', icon: 'none' })
    showCancel.value = false
    load()
  } catch (e) {} finally { canceling.value = false }
}
</script>

<template>
  <view class="page-wrap detail-page">
    <view class="subnav" @click="back">
      <AppIcon name="chevron-left" :size="16" color="#0071e3" />
      <text>返回</text>
    </view>

    <view v-if="loading" class="loading-block">
      <view class="spinner" />
      <text>加载中…</text>
    </view>
    <view v-else-if="failed" class="card">
      <EmptyBox icon="warning" text="加载失败或无权查看该请假单" />
    </view>

    <template v-else-if="detail">
      <view class="page-head">
        <view style="flex: 1; min-width: 0">
          <view class="large-title">{{ detail.typeText || typeText(detail.type) }}申请</view>
          <view class="page-subtitle">{{ detail.studentName }} · {{ detail.className || '-' }}</view>
        </view>
        <StatusPill :status="detail.status" />
      </view>

      <!-- 基本信息 -->
      <view class="card" style="margin-bottom: 16px">
        <view class="section-title">请假信息</view>
        <view class="group-row info-row"><text class="k">请假类型</text><text class="v">{{ detail.typeText || typeText(detail.type) }}</text></view>
        <view class="group-row info-row"><text class="k">开始时间</text><text class="v">{{ fmtTime(detail.startTime, true) }}</text></view>
        <view class="group-row info-row"><text class="k">结束时间</text><text class="v">{{ fmtTime(detail.endTime, true) }}</text></view>
        <view class="group-row info-row"><text class="k">请假天数</text><text class="v">{{ detail.days }} 天</text></view>
        <view class="group-row info-row"><text class="k">请假事由</text><text class="v">{{ detail.reason }}</text></view>
        <view v-if="detail.destination" class="group-row info-row"><text class="k">目的地</text><text class="v">{{ detail.destination }}</text></view>
        <view v-if="detail.contactPhone" class="group-row info-row"><text class="k">联系电话</text><text class="v">{{ detail.contactPhone }}</text></view>
        <view class="group-row info-row"><text class="k">提交时间</text><text class="v">{{ fmtTime(detail.createTime, true) }}</text></view>
        <view v-if="detail.approverName" class="group-row info-row"><text class="k">审批人</text><text class="v">{{ detail.approverName }}</text></view>
        <view v-if="detail.approveComment" class="group-row info-row"><text class="k">审批意见</text><text class="v">{{ detail.approveComment }}</text></view>
        <view v-if="detail.cancelNote" class="group-row info-row"><text class="k">销假说明</text><text class="v">{{ detail.cancelNote }}</text></view>

        <view v-if="role === 'STUDENT' && ['PENDING', 'APPROVED'].includes(detail.status)" class="detail-actions">
          <view v-if="detail.status === 'PENDING'" class="btn btn-danger-text" @click="showRevoke = true">
            <AppIcon name="undo" :size="15" color="#ff3b30" />
            <text>撤回申请</text>
          </view>
          <view v-if="detail.status === 'APPROVED'" class="btn btn-primary" @click="showCancel = true">
            <AppIcon name="check" :size="15" color="#ffffff" />
            <text>申请销假</text>
          </view>
        </view>
      </view>

      <!-- 请假证明附件 -->
      <view class="card" style="margin-bottom: 16px">
        <view class="attach-head">
          <text class="section-title-inline">请假证明</text>
          <view v-if="isOwner" class="btn btn-sm btn-primary" :class="{ disabled: uploading }" @click="chooseAndUpload">
            <AppIcon name="upload" :size="14" color="#ffffff" />
            <text>{{ uploading ? '上传中…' : '上传证明' }}</text>
          </view>
        </view>
        <view v-if="attachments.length" class="attach-list">
          <view v-for="a in attachments" :key="a.id || a.fileUrl" class="group-row attach-row" @click="previewAttachment(a)">
            <view class="attach-icon">
              <AppIcon :name="isImage(a) ? 'image' : 'doc'" :size="16" color="#0071e3" />
            </view>
            <view class="attach-main">
              <view class="attach-name">{{ a.fileName || '附件' }}</view>
              <view v-if="a.uploadTime" class="attach-time">{{ fmtTime(a.uploadTime, true) }}</view>
            </view>
            <AppIcon v-if="isImage(a)" name="chevron-right" :size="15" color="#c7c7cc" />
          </view>
        </view>
        <view v-else class="attach-empty">
          <text>{{ isOwner ? '暂无证明，可上传病历 / 车票等图片' : '暂无请假证明' }}</text>
        </view>
      </view>

      <!-- 审批时间线 -->
      <view class="card">
        <view class="section-title">审批时间线</view>
        <view v-if="detail.records?.length" class="timeline">
          <view v-for="(r, i) in detail.records" :key="i" class="tl-item">
            <view class="tl-rail">
              <view class="tl-dot" :style="{ background: actionInfo(r.action).color }" />
              <view v-if="i < detail.records.length - 1" class="tl-line" />
            </view>
            <view class="tl-body">
              <view class="tl-head">
                <text class="tl-action" :style="{ color: actionInfo(r.action).color }">{{ r.actionText || actionInfo(r.action).text }}</text>
                <text class="tl-time">{{ fmtTime(r.createTime, true) }}</text>
              </view>
              <view class="tl-operator">{{ r.operatorName }}</view>
              <view v-if="r.comment" class="tl-comment">{{ r.comment }}</view>
            </view>
          </view>
        </view>
        <EmptyBox v-else icon="clock" text="暂无审批记录" />
      </view>
    </template>

    <!-- 撤回确认 -->
    <SheetModal :show="showRevoke" title="撤回申请" @close="showRevoke = false">
      <view class="confirm-text">确定撤回这条请假申请吗？撤回后不可恢复。</view>
      <template #footer>
        <view class="btn btn-secondary" @click="showRevoke = false">取消</view>
        <view class="btn btn-danger" :class="{ disabled: revoking }" @click="doRevoke">确认撤回</view>
      </template>
    </SheetModal>

    <!-- 销假申请 -->
    <SheetModal :show="showCancel" title="申请销假" @close="showCancel = false">
      <view class="field">
        <text class="field-label">销假说明</text>
        <textarea v-model="cancelNote" class="textarea" placeholder="如：已于15日晚返校" placeholder-class="ph" />
      </view>
      <template #footer>
        <view class="btn btn-secondary" @click="showCancel = false">取消</view>
        <view class="btn btn-primary" :class="{ disabled: canceling }" @click="doCancelApply">提交销假</view>
      </template>
    </SheetModal>
  </view>
</template>

<style scoped>
.detail-page { padding-bottom: 40px; }
.page-head { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; }
.section-title { font-size: 15px; font-weight: 700; padding: 16px 18px 6px; }
.info-row { justify-content: space-between; gap: 20px; }
.info-row .k { font-size: 13.5px; color: var(--text-2); flex-shrink: 0; }
.info-row .v { font-size: 14px; font-weight: 500; text-align: right; word-break: break-all; }
.detail-actions {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 12px 18px 16px;
  border-top: .5px solid var(--separator);
  margin-top: 4px;
}

.attach-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 18px 8px;
}
.section-title-inline { font-size: 15px; font-weight: 700; }
.attach-list { padding-bottom: 6px; }
.attach-row { gap: 12px; }
.attach-icon {
  width: 34px; height: 34px; border-radius: 10px;
  background: var(--accent-soft);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.attach-main { flex: 1; min-width: 0; }
.attach-name {
  font-size: 14px; font-weight: 500; color: var(--text-1);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.attach-time { font-size: 11.5px; color: var(--text-2); margin-top: 2px; }
.attach-empty { padding: 4px 18px 18px; font-size: 13px; color: var(--text-2); }

.timeline { padding: 8px 18px 20px; }
.tl-item { display: flex; gap: 14px; }
.tl-rail { display: flex; flex-direction: column; align-items: center; }
.tl-dot {
  width: 11px; height: 11px; border-radius: 50%;
  margin-top: 5px;
  box-shadow: 0 0 0 3.5px rgba(0, 0, 0, .05);
  flex-shrink: 0;
}
.tl-line { width: 2px; flex: 1; background: var(--separator); margin: 5px 0 1px; min-height: 18px; }
.tl-body { padding-bottom: 18px; flex: 1; min-width: 0; }
.tl-head { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; }
.tl-action { font-size: 14px; font-weight: 600; }
.tl-time { font-size: 11.5px; color: var(--text-2); white-space: nowrap; }
.tl-operator { font-size: 12.5px; color: var(--text-2); margin-top: 1px; }
.tl-comment {
  margin-top: 7px;
  font-size: 13px;
  background: rgba(0, 0, 0, .04);
  border-radius: 10px;
  padding: 8px 12px;
  color: var(--text-1);
}
.confirm-text { color: var(--text-2); font-size: 14px; line-height: 1.7; }
</style>
