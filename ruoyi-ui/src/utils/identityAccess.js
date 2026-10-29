export function canUse(permissions, permission) {
  return (
    Array.isArray(permissions) &&
    (permissions.indexOf('*:*:*') !== -1 || permissions.indexOf(permission) !== -1)
  )
}

// Decode text entities for text interpolation only. Never render expressions as HTML.
export function displayExpression(value) {
  if (value == null) {
    return ''
  }
  return String(value)
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
}

export function parseTimeCondition(policy) {
  if (!policy || !['1', 'DENY'].includes(String(policy.effect).toUpperCase())) return null
  const match = /^time\s+not\s+in\s*\[\s*([0-9]{2}:[0-9]{2})\s*-\s*([0-9]{2}:[0-9]{2})\s*\]$/i.exec(String(policy.envExpr || '').trim())
  if (!match || match[1] === match[2] || ![match[1], match[2]].every(time => /^(?:[01][0-9]|2[0-3]):[0-5][0-9]$/.test(time))) return null
  const resource = String(policy.resourceExpr || '').trim()
  let levels = []
  if (resource) {
    const scope = /^level\s+in\s*\[\s*((?:PUBLIC|INTERNAL|SECRET|CONFIDENTIAL)(?:\s*,\s*(?:PUBLIC|INTERNAL|SECRET|CONFIDENTIAL))*)\s*\]$/.exec(resource)
    if (!scope) return null
    levels = scope[1].split(',').map(level => level.trim())
    if (new Set(levels).size !== levels.length) return null
  }
  return { start: match[1], end: match[2], levels }
}

export function hasSupportedTimeCondition(policy) {
  return !!parseTimeCondition(policy)
}

export function timeConditionSummary(policy) {
  const condition = parseTimeCondition(policy)
  if (!condition) return ''
  const labels = { PUBLIC: '公开', INTERNAL: '内部', SECRET: '秘密', CONFIDENTIAL: '机密' }
  const scope = condition.levels.length ? condition.levels.map(level => labels[level]).join('、') : '全部文档'
  return scope + '：限 ' + condition.start + '—' + condition.end + '访问' + (condition.start > condition.end ? '（跨日）' : '')
}
