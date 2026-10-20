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

// Mirrors the limited condition recognized by the current backend; not a policy decision.
export function hasSupportedTimeCondition(policy) {
  if (!policy || !['1', 'DENY'].includes(String(policy.effect).toUpperCase())) return false
  return !String(policy.resourceExpr || '').trim() &&
    /^time\s+not\s+in\s*\[\s*09:00\s*-\s*18:00\s*\]$/i.test(String(policy.envExpr || '').trim())
}
