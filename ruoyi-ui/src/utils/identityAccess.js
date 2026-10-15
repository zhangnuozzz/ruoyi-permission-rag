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
  const expression = policy && policy.envExpr
  return (
    !!policy &&
    policy.effect === '1' &&
    typeof expression === 'string' &&
    expression.indexOf('time not in') !== -1 &&
    expression.indexOf('09:00-18:00') !== -1
  )
}
