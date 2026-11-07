export const passwordMessage = '10至64位，包含大写字母、小写字母和数字，可使用英文符号'
export function validPassword(value) {
  return /^[\x21-\x7E]{10,64}$/.test(value || '') && /[A-Z]/.test(value) && /[a-z]/.test(value) && /[0-9]/.test(value)
}
