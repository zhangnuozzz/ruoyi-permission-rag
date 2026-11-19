export const passwordMessage = '10至64位，包含大写字母、小写字母、数字和英文符号中的至少两类'

export function validPassword(value) {
  if (typeof value !== 'string' || value.length < 10 || value.length > 64 || /[^\x21-\x7E]/.test(value)) {
    return false
  }
  return [/[A-Z]/, /[a-z]/, /[0-9]/, /[^A-Za-z0-9]/].filter(type => type.test(value)).length >= 2
}
