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

export function parseAccessCondition(policy) {
  if (!policy || !['0', '1', 'ALLOW', 'DENY'].includes(String(policy.effect).toUpperCase())) return null
  const allow = ['0', 'ALLOW'].includes(String(policy.effect).toUpperCase())
  if (allow && policy.conditionVersion !== 1) return null
  const resource = String(policy.resourceExpr || '').trim()
  const result = { start: null, end: null, networks: [], maxRisk: null, levels: [], types: [] }
  if (resource) {
    const match = /^level\s+in\s*\[\s*((?:PUBLIC|INTERNAL|SECRET|CONFIDENTIAL)(?:\s*,\s*(?:PUBLIC|INTERNAL|SECRET|CONFIDENTIAL))*)\s*\]$/.exec(resource)
    if (!match) return null
    result.levels = match[1].split(',').map(level => level.trim())
    if (new Set(result.levels).size !== result.levels.length) return null
  }
  let expression = String(policy.envExpr || '').trim()
  if (allow) {
    if (expression.includes('||') || /\b(?:time|ip)\s+not\s+in\b|\brisk\s+above\b/i.test(expression)) return null
    expression = expression.replace(/&&/g, '||').replace(/\btime\s+in\b/gi, 'time not in').replace(/\bip\s+in\b/gi, 'ip not in').replace(/\brisk\s+at\s+most\b/gi, 'risk above')
  } else if (expression.includes('&&')) return null
  if (!expression || expression.length > 500) return null
  const clauses = expression.split(/\s*\|\|\s*/)
  if (clauses.length > 3) return null
  for (const clause of clauses) {
    const time = /^time\s+not\s+in\s*\[\s*([0-9]{2}:[0-9]{2})\s*-\s*([0-9]{2}:[0-9]{2})\s*\]$/i.exec(clause)
    const network = /^ip\s+not\s+in\s*\[\s*([^\[\]]+)\s*\]$/i.exec(clause)
    const risk = /^risk\s+above\s*\[\s*(LOW|MEDIUM|HIGH)\s*\]$/i.exec(clause)
    if (time && !result.types.includes('TIME')) {
      if (time[1] === time[2] || ![time[1], time[2]].every(value => /^(?:[01][0-9]|2[0-3]):[0-5][0-9]$/.test(value))) return null
      result.start = time[1]; result.end = time[2]; result.types.push('TIME')
    } else if (network && !result.types.includes('IP')) {
      result.networks = network[1].split(',').map(value => value.trim())
      if (!result.networks.length || result.networks.length > 32 || !result.networks.every(validNetwork)) return null
      result.types.push('IP')
    } else if (risk && !result.types.includes('RISK')) {
      result.maxRisk = risk[1].toUpperCase(); result.types.push('RISK')
    } else return null
  }
  return result
}

export function validNetwork(value) {
  const parts = value.split('/')
  if (parts.length > 2) return false
  const address = parts[0]
  const ipv4 = address.split('.')
  const isV4 = ipv4.length === 4 && ipv4.every(part => /^(?:0|[1-9][0-9]{0,2})$/.test(part) && Number(part) <= 255)
  let isV6 = false
  if (address.includes(':') && /^[0-9a-fA-F:]+$/.test(address) && address.length <= 39) {
    const halves = address.split('::')
    const groups = address.split(':').filter(Boolean)
    isV6 = halves.length <= 2 && groups.every(group => /^[0-9a-fA-F]{1,4}$/.test(group)) &&
      (halves.length === 2 ? groups.length < 8 : groups.length === 8)
    if (halves.length === 1 && (address.startsWith(':') || address.endsWith(':'))) isV6 = false
    if (address.includes(':::')) isV6 = false
  }
  if (!isV4 && !isV6) return false
  return parts.length === 1 || /^(?:0|[1-9][0-9]{0,2})$/.test(parts[1]) && Number(parts[1]) <= (isV4 ? 32 : 128)
}

export function hasSupportedAccessCondition(policy) {
  return !!parseAccessCondition(policy)
}

export function accessConditionSummary(policy) {
  const condition = parseAccessCondition(policy)
  if (!condition) return ''
  const labels = { PUBLIC: '公开', INTERNAL: '内部', SECRET: '秘密', CONFIDENTIAL: '机密' }
  const scope = condition.levels.length ? condition.levels.map(level => labels[level]).join('、') : '全部文档'
  const constraints = []
  if (condition.types.includes('TIME')) constraints.push(condition.start + '—' + condition.end + (condition.start > condition.end ? '（跨日）' : ''))
  if (condition.types.includes('IP')) constraints.push('限定 ' + condition.networks.length + ' 个IP地址或网段')
  if (condition.types.includes('RISK')) constraints.push('风险不高于' + { LOW: '低', MEDIUM: '中', HIGH: '高' }[condition.maxRisk])
  return scope + '：' + (['0', 'ALLOW'].includes(String(policy.effect).toUpperCase()) ? '命中时允许；' : '不满足时拒绝；') + constraints.join('；')
}
