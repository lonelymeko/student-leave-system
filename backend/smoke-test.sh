#!/usr/bin/env bash
# ============================================================
# 学生请销假系统 — 后端冒烟测试
# 前置：服务已启动 (mvn spring-boot:run)，Base URL http://localhost:8080/api
# 依赖：curl、python3（美化/断言 JSON）
# 用法：bash smoke-test.sh
# ============================================================
set -u
BASE="http://localhost:8080/api"
PASS=0; FAIL=0

say()  { printf '\n\033[1;36m== %s ==\033[0m\n' "$*"; }
json() { python3 -m json.tool 2>/dev/null || cat; }

# assert_code <期望code> <响应json> <说明>
assert_code() {
  local expect="$1" resp="$2" name="$3"
  local code
  code=$(printf '%s' "$resp" | python3 -c "import sys,json;print(json.load(sys.stdin).get('code'))" 2>/dev/null)
  if [ "$code" = "$expect" ]; then
    echo "  [PASS] $name (code=$code)"; PASS=$((PASS+1))
  else
    echo "  [FAIL] $name 期望 code=$expect 实际 code=$code"; echo "  resp: $resp"; FAIL=$((FAIL+1))
  fi
}

field() { printf '%s' "$1" | python3 -c "
import sys,json
d=json.load(sys.stdin)
for k in '$2'.split('.'):
    d=d[int(k)] if isinstance(d,list) else d.get(k)
    if d is None: break
print(d if d is not None else '')" 2>/dev/null; }

login() { # login <username> <password> -> token
  curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}"
}

say "1. 三角色登录"
R=$(login admin admin123);   assert_code 0 "$R" "admin 登录";   ADMIN_TOKEN=$(field "$R" data.token)
R=$(login teacher1 123456);  assert_code 0 "$R" "teacher1 登录"; TEACHER_TOKEN=$(field "$R" data.token)
R=$(login student1 123456);  assert_code 0 "$R" "student1 登录"; STUDENT_TOKEN=$(field "$R" data.token)
R=$(login student1 wrongpwd); assert_code 4001 "$R" "错误密码返回 4001"

say "2. GET /auth/me"
R=$(curl -s "$BASE/auth/me" -H "Authorization: Bearer $STUDENT_TOKEN")
assert_code 0 "$R" "student1 /auth/me"
echo "  user: $(field "$R" data.realName) / $(field "$R" data.role) / 辅导员=$(field "$R" data.teacherName)"

say "3. 未带 token 访问 → 401"
R=$(curl -s "$BASE/leave/my")
assert_code 401 "$R" "无 token 返回 401"

say "4. student1 提交请假单"
START=$(date -d "+2 day" '+%Y-%m-%d 08:00:00')
END=$(date -d "+4 day" '+%Y-%m-%d 18:00:00')
R=$(curl -s -X POST "$BASE/leave" -H "Authorization: Bearer $STUDENT_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"type\":\"PERSONAL\",\"startTime\":\"$START\",\"endTime\":\"$END\",\"reason\":\"回家参加表哥婚礼\",\"destination\":\"乌鲁木齐\",\"contactPhone\":\"13800000000\"}")
assert_code 0 "$R" "提交请假"
LEAVE_ID=$(field "$R" data.id)
echo "  新单 id=$LEAVE_ID days=$(field "$R" data.days) status=$(field "$R" data.status)"

say "4b. 参数校验：reason 太短 → 4001"
R=$(curl -s -X POST "$BASE/leave" -H "Authorization: Bearer $STUDENT_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"type\":\"PERSONAL\",\"startTime\":\"$START\",\"endTime\":\"$END\",\"reason\":\"短\"}")
assert_code 4001 "$R" "reason<5字 返回 4001"

say "5. student1 我的请假列表"
R=$(curl -s "$BASE/leave/my?page=1&size=10" -H "Authorization: Bearer $STUDENT_TOKEN")
assert_code 0 "$R" "我的请假分页"
echo "  total=$(field "$R" data.total)"

say "6. teacher1 待办（新单应在列表中）"
R=$(curl -s "$BASE/approval/pending?page=1&size=10" -H "Authorization: Bearer $TEACHER_TOKEN")
assert_code 0 "$R" "待办列表"
echo "  待办 total=$(field "$R" data.total) 第一条 status=$(field "$R" data.records.0.status)"

say "7. teacher1 审批通过"
R=$(curl -s -X POST "$BASE/approval/$LEAVE_ID" -H "Authorization: Bearer $TEACHER_TOKEN" -H 'Content-Type: application/json' \
  -d '{"action":"APPROVE","comment":"同意，注意安全"}')
assert_code 0 "$R" "审批通过"
echo "  status=$(field "$R" data.status)"

say "7b. 重复审批已通过的单 → 4009"
R=$(curl -s -X POST "$BASE/approval/$LEAVE_ID" -H "Authorization: Bearer $TEACHER_TOKEN" -H 'Content-Type: application/json' \
  -d '{"action":"APPROVE","comment":"再批一次"}')
assert_code 4009 "$R" "非 PENDING 审批返回 4009"

say "7c. 越权：student1 撤回已通过的单 → 4009；操作他人单 → 403"
R=$(curl -s -X PUT "$BASE/leave/$LEAVE_ID/revoke" -H "Authorization: Bearer $STUDENT_TOKEN")
assert_code 4009 "$R" "APPROVED 撤回返回 4009"
R=$(curl -s "$BASE/leave/10" -H "Authorization: Bearer $STUDENT_TOKEN")
assert_code 403 "$R" "student1 查看他人单返回 403"

say "8. student1 申请销假"
R=$(curl -s -X POST "$BASE/leave/$LEAVE_ID/cancel-apply" -H "Authorization: Bearer $STUDENT_TOKEN" -H 'Content-Type: application/json' \
  -d '{"note":"已提前返校"}')
assert_code 0 "$R" "申请销假"

say "9. teacher1 确认销假 → COMPLETED"
R=$(curl -s -X POST "$BASE/approval/$LEAVE_ID/cancel-confirm" -H "Authorization: Bearer $TEACHER_TOKEN")
assert_code 0 "$R" "确认销假"
echo "  status=$(field "$R" data.status)"

say "9b. 详情时间线"
R=$(curl -s "$BASE/leave/$LEAVE_ID" -H "Authorization: Bearer $STUDENT_TOKEN")
assert_code 0 "$R" "详情+时间线"
printf '%s' "$R" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
print('  状态:', d['status'], d['statusText'])
for r in d['records']:
    print('   -', r['createTime'], r['operatorName'], r['actionText'], r.get('comment') or '')"

say "10. admin 统计 / 用户 / 全部请假单"
R=$(curl -s "$BASE/admin/stats/overview" -H "Authorization: Bearer $ADMIN_TOKEN")
assert_code 0 "$R" "统计 overview"
echo "  totalCount=$(field "$R" data.totalCount) pending=$(field "$R" data.pendingCount) approved=$(field "$R" data.approvedCount) completed=$(field "$R" data.completedCount)"
R=$(curl -s "$BASE/admin/users?page=1&size=20" -H "Authorization: Bearer $ADMIN_TOKEN")
assert_code 0 "$R" "用户分页"
R=$(curl -s "$BASE/admin/leaves?page=1&size=5" -H "Authorization: Bearer $ADMIN_TOKEN")
assert_code 0 "$R" "全部请假单"
R=$(curl -s "$BASE/admin/teachers" -H "Authorization: Bearer $ADMIN_TOKEN")
assert_code 0 "$R" "辅导员下拉"

say "10b. 角色鉴权：student 访问 admin 接口 → 403"
R=$(curl -s "$BASE/admin/users" -H "Authorization: Bearer $STUDENT_TOKEN")
assert_code 403 "$R" "student 访问 /admin 返回 403"

say "11. AI 接口（无 Key 环境应优雅降级 5001）"
R=$(curl -s -X POST "$BASE/ai/draft" -H "Authorization: Bearer $STUDENT_TOKEN" -H 'Content-Type: application/json' \
  -d '{"text":"我下周一到周三回家参加表哥的婚礼，去乌鲁木齐"}')
if [ -n "${ANTHROPIC_API_KEY:-}" ] && [ "${ANTHROPIC_API_KEY:-sk-placeholder}" != "sk-placeholder" ]; then
  assert_code 0 "$R" "AI draft（已配 Key）"
else
  assert_code 5001 "$R" "AI draft 无 Key 降级 5001"
fi
R=$(curl -s -X POST "$BASE/ai/chat" -H "Authorization: Bearer $STUDENT_TOKEN" -H 'Content-Type: application/json' \
  -d '{"message":"病假超过3天需要什么材料？"}')
if [ -n "${ANTHROPIC_API_KEY:-}" ] && [ "${ANTHROPIC_API_KEY:-sk-placeholder}" != "sk-placeholder" ]; then
  assert_code 0 "$R" "AI chat（已配 Key）"
else
  assert_code 5001 "$R" "AI chat 无 Key 降级 5001"
fi

printf '\n\033[1m===== 冒烟结果: PASS=%d FAIL=%d =====\033[0m\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
