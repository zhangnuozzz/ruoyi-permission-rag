#!/usr/bin/env bash
#
# 验证RAG文件切块处理是否写入Milvus、MariaDB和MinIO
# author: fufu
# date: 2026-05-05

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
RAG_SECURITY_LEVEL="${RAG_SECURITY_LEVEL:-internal}"
RAG_AUTH_TOKEN="${RAG_AUTH_TOKEN:-}"
RAG_USERNAME="${RAG_USERNAME:-admin}"
RAG_PASSWORD="${RAG_PASSWORD:-admin123}"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-ruoyi}"
DB_USER="${DB_USER:-fufu}"
DB_PASSWORD="${DB_PASSWORD:-Eee2.71828}"

MILVUS_REST_URL="${MILVUS_REST_URL:-http://localhost:9091}"
MILVUS_COLLECTION="${MILVUS_COLLECTION:-rag_file_chunks}"

MINIO_ALIAS="${MINIO_ALIAS:-ragverify}"
MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://localhost:9000}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-minioadmin}"
MINIO_BUCKET="${MINIO_BUCKET:-rag-files}"
RAG_DISABLE_PROXY="${RAG_DISABLE_PROXY:-false}"
RAG_CURL_RETRIES="${RAG_CURL_RETRIES:-20}"
RAG_CURL_RETRY_DELAY="${RAG_CURL_RETRY_DELAY:-1}"

if [[ "${RAG_DISABLE_PROXY}" == "true" ]]; then
  export no_proxy="*"
  export NO_PROXY="*"
fi

curl() {
  local attempt=1
  local last_status=0
  local err_file
  err_file="$(mktemp)"
  while (( attempt <= RAG_CURL_RETRIES )); do
    : > "${err_file}"
    command curl "$@" 2>"${err_file}"
    last_status=$?
    if (( last_status == 0 )); then
      rm -f "${err_file}"
      return 0
    fi
    if (( attempt < RAG_CURL_RETRIES )); then
      sleep "${RAG_CURL_RETRY_DELAY}"
    fi
    attempt=$((attempt + 1))
  done
  cat "${err_file}" >&2
  rm -f "${err_file}"
  return "${last_status}"
}

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <test-file>"
  echo
  echo "Optional environment variables:"
  echo "  API_BASE_URL, RAG_AUTH_TOKEN, RAG_USERNAME, RAG_PASSWORD, RAG_SECURITY_LEVEL"
  echo "  DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD"
  echo "  MILVUS_REST_URL, MILVUS_COLLECTION"
  echo "  MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET"
  echo "  RAG_DISABLE_PROXY=true to bypass HTTP_PROXY/HTTPS_PROXY for local checks"
  echo "  RAG_CURL_RETRIES, RAG_CURL_RETRY_DELAY"
  exit 2
fi

TEST_FILE="$1"
if [[ ! -f "${TEST_FILE}" ]]; then
  echo "ERROR: test file not found: ${TEST_FILE}" >&2
  exit 2
fi
EXPECTED_SNIPPET="$(tr '\r\n\t' '   ' < "${TEST_FILE}" | tr -s ' ' | sed 's/^ //;s/ $//' | head -c 40 || true)"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

REPORT_FILE="${TMP_DIR}/report.txt"
UPLOAD_RESPONSE="${TMP_DIR}/upload-response.json"
CAPTCHA_RESPONSE="${TMP_DIR}/captcha-response.json"
LOGIN_RESPONSE="${TMP_DIR}/login-response.json"
MILVUS_RESPONSE="${TMP_DIR}/milvus-response.json"
MINIO_OBJECT="${TMP_DIR}/minio-object.bin"

PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

section() {
  printf '\n## %s\n' "$1" | tee -a "${REPORT_FILE}"
}

pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  printf '[PASS] %s\n' "$1" | tee -a "${REPORT_FILE}"
}

fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  printf '[FAIL] %s\n' "$1" | tee -a "${REPORT_FILE}"
}

warn() {
  WARN_COUNT=$((WARN_COUNT + 1))
  printf '[WARN] %s\n' "$1" | tee -a "${REPORT_FILE}"
}

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    fail "Missing command: $1"
    return 1
  fi
  pass "Command available: $1"
}

json_get() {
  jq -r "$1 // empty" "$2"
}

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

mysql_query() {
  mysql --batch --raw --skip-column-names \
    -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" \
    -e "$1"
}

print_json() {
  local file="$1"
  if [[ -s "${file}" ]]; then
    jq . "${file}" 2>/dev/null || cat "${file}"
  else
    echo "<empty>"
  fi
}

minio_java_verify() {
  local object_name="$1"
  local expected_file="$2"
  local classpath_file="${TMP_DIR}/minio-classpath.txt"
  local java_file="${TMP_DIR}/MinioObjectVerifier.java"

  if ! command -v mvn >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1 || ! command -v java >/dev/null 2>&1; then
    echo "Missing mvn/javac/java for MinIO Java SDK fallback."
    return 1
  fi

  if ! mvn -q -pl ruoyi-system -am -DskipTests compile dependency:build-classpath -Dmdep.outputFile="${classpath_file}" >/dev/null; then
    echo "Could not build Maven classpath for MinIO Java SDK fallback."
    return 1
  fi

  cat > "${java_file}" <<'JAVA'
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class MinioObjectVerifier {
    public static void main(String[] args) throws Exception {
        MinioClient client = MinioClient.builder()
            .endpoint(args[0])
            .credentials(args[1], args[2])
            .build();
        String bucket = args[3];
        String objectName = args[4];
        byte[] expected = Files.readAllBytes(Path.of(args[5]));
        byte[] actual;
        try (InputStream inputStream = client.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build())) {
            actual = inputStream.readAllBytes();
        }
        if (!Arrays.equals(expected, actual)) {
            System.out.println("MinIO object exists but content differs. expectedBytes="
                + expected.length + ", actualBytes=" + actual.length);
            System.exit(2);
        }
        System.out.println("MinIO object exists and content matches. object="
            + bucket + "/" + objectName + ", bytes=" + actual.length);
    }
}
JAVA

  if ! javac -cp "$(cat "${classpath_file}")" "${java_file}"; then
    echo "Could not compile MinIO Java SDK fallback verifier."
    return 1
  fi

  java -cp "$(cat "${classpath_file}"):${TMP_DIR}" MinioObjectVerifier \
    "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" \
    "${MINIO_BUCKET}" "${object_name}" "${expected_file}"
}

finish_failure() {
  section "Summary"
  echo "PASS=${PASS_COUNT}" | tee -a "${REPORT_FILE}"
  echo "FAIL=${FAIL_COUNT}" | tee -a "${REPORT_FILE}"
  echo "WARN=${WARN_COUNT}" | tee -a "${REPORT_FILE}"
  exit 1
}

auth_header_value() {
  if [[ "${TOKEN}" == Bearer\ * ]]; then
    printf "%s" "${TOKEN}"
  else
    printf "Bearer %s" "${TOKEN}"
  fi
}

echo "RAG file processing verification report" | tee "${REPORT_FILE}"
echo "author: fufu" | tee -a "${REPORT_FILE}"
echo "date: 2026-05-05" | tee -a "${REPORT_FILE}"
echo "run_at: $(date '+%Y-%m-%d %H:%M:%S')" | tee -a "${REPORT_FILE}"
echo "project_root: ${PROJECT_ROOT}" | tee -a "${REPORT_FILE}"
echo "test_file: ${TEST_FILE}" | tee -a "${REPORT_FILE}"
echo "api_base_url: ${API_BASE_URL}" | tee -a "${REPORT_FILE}"

section "Prerequisites"
need_cmd curl || true
need_cmd jq || true
need_cmd mysql || true
if command -v mc >/dev/null 2>&1; then
  pass "Command available: mc"
elif command -v mvn >/dev/null 2>&1 && command -v javac >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
  pass "mc not installed; Java SDK fallback available for MinIO verification"
else
  warn "Command missing: mc. MinIO verification will use Java SDK fallback."
fi

if (( FAIL_COUNT > 0 )); then
  section "Summary"
  fail "Prerequisite checks failed; cannot continue."
  finish_failure
fi

section "Authentication"
if [[ -n "${RAG_AUTH_TOKEN}" ]]; then
  TOKEN="${RAG_AUTH_TOKEN}"
  pass "Using token from RAG_AUTH_TOKEN"
else
  echo "Fetching captcha settings..." | tee -a "${REPORT_FILE}"
  if ! curl -fsS "${API_BASE_URL}/captchaImage" -o "${CAPTCHA_RESPONSE}"; then
    fail "Could not fetch captcha settings from ${API_BASE_URL}/captchaImage"
    finish_failure
  fi
  CAPTCHA_ENABLED="$(json_get '.captchaEnabled' "${CAPTCHA_RESPONSE}")"
  if [[ "${CAPTCHA_ENABLED}" == "true" ]]; then
    fail "Captcha is enabled. Set RAG_AUTH_TOKEN to a valid backend token, or temporarily disable captcha for scripted verification."
    echo "Captcha response:" | tee -a "${REPORT_FILE}"
    print_json "${CAPTCHA_RESPONSE}" | tee -a "${REPORT_FILE}"
    finish_failure
  fi

  LOGIN_PAYLOAD="$(jq -n --arg username "${RAG_USERNAME}" --arg password "${RAG_PASSWORD}" '{username:$username,password:$password,code:"",uuid:""}')"
  if ! curl -fsS -H 'Content-Type: application/json' -d "${LOGIN_PAYLOAD}" "${API_BASE_URL}/login" -o "${LOGIN_RESPONSE}"; then
    fail "Login request failed"
    finish_failure
  fi
  TOKEN="$(json_get '.token' "${LOGIN_RESPONSE}")"
  if [[ -z "${TOKEN}" ]]; then
    fail "Login did not return token"
    echo "Login response:" | tee -a "${REPORT_FILE}"
    print_json "${LOGIN_RESPONSE}" | tee -a "${REPORT_FILE}"
    finish_failure
  fi
  pass "Logged in as ${RAG_USERNAME}"
fi

section "Upload"
AUTH_HEADER="$(auth_header_value)"
if ! curl -fsS \
  -H "Authorization: ${AUTH_HEADER}" \
  -F "file=@${TEST_FILE}" \
  -F "securityLevel=${RAG_SECURITY_LEVEL}" \
  "${API_BASE_URL}/rag/file/upload" \
  -o "${UPLOAD_RESPONSE}"; then
  fail "Upload request failed"
  finish_failure
fi

UPLOAD_CODE="$(json_get '.code' "${UPLOAD_RESPONSE}")"
FILE_ID="$(json_get '.data.fileId' "${UPLOAD_RESPONSE}")"
FILE_NAME="$(json_get '.data.fileName' "${UPLOAD_RESPONSE}")"
CHUNK_COUNT="$(json_get '.data.chunkCount' "${UPLOAD_RESPONSE}")"
GROUP_ID="$(json_get '.data.groupId' "${UPLOAD_RESPONSE}")"
GROUP_NAME="$(json_get '.data.groupName' "${UPLOAD_RESPONSE}")"
MINIO_OBJECT_NAME="$(json_get '.data.minioObjectName' "${UPLOAD_RESPONSE}")"

echo "Upload response:" | tee -a "${REPORT_FILE}"
print_json "${UPLOAD_RESPONSE}" | tee -a "${REPORT_FILE}"

if [[ "${UPLOAD_CODE}" == "200" && -n "${FILE_ID}" ]]; then
  pass "Upload returned fileId=${FILE_ID}, chunkCount=${CHUNK_COUNT}"
else
  fail "Upload response did not contain success code and fileId"
  finish_failure
fi

section "MariaDB Verification"
DB_ROW="$(mysql_query "select file_id, file_name, upload_user_name, security_level, group_id, group_name, minio_object_name, date_format(create_time, '%Y-%m-%d %H:%i:%s') from sys_rag_file where file_id='$(sql_escape "${FILE_ID}")';" || true)"
if [[ -n "${DB_ROW}" ]]; then
  pass "sys_rag_file row found"
  echo "${DB_ROW}" | tee -a "${REPORT_FILE}"
else
  fail "No sys_rag_file row found for file_id=${FILE_ID}"
fi

DB_SECURITY_LEVEL="$(printf '%s\n' "${DB_ROW}" | awk -F'\t' '{print $4}')"
DB_GROUP_ID="$(printf '%s\n' "${DB_ROW}" | awk -F'\t' '{print $5}')"
DB_MINIO_OBJECT="$(printf '%s\n' "${DB_ROW}" | awk -F'\t' '{print $7}')"

if [[ "${DB_SECURITY_LEVEL}" == "${RAG_SECURITY_LEVEL}" ]]; then
  pass "MariaDB security_level matches ${RAG_SECURITY_LEVEL}"
else
  fail "MariaDB security_level mismatch: expected ${RAG_SECURITY_LEVEL}, got ${DB_SECURITY_LEVEL:-<empty>}"
fi

DB_FILE_NAME="$(printf '%s\n' "${DB_ROW}" | awk -F'\t' '{print $2}')"
if [[ "${DB_FILE_NAME}" == "$(basename "${TEST_FILE}")" || "${DB_FILE_NAME}" == "${FILE_NAME}" ]]; then
  pass "MariaDB file_name matches uploaded file"
else
  fail "MariaDB file_name mismatch: upload=${FILE_NAME:-<empty>}, db=${DB_FILE_NAME:-<empty>}"
fi

if [[ "${DB_GROUP_ID}" == "${GROUP_ID}" && -n "${GROUP_ID}" ]]; then
  pass "MariaDB group_id matches upload result: ${GROUP_ID}"
else
  fail "MariaDB group_id mismatch: upload=${GROUP_ID:-<empty>}, db=${DB_GROUP_ID:-<empty>}"
fi

section "Milvus Verification"
MILVUS_FILTER="file_id == \"${FILE_ID}\""
MILVUS_PAYLOAD_V2="$(jq -n \
  --arg collectionName "${MILVUS_COLLECTION}" \
  --arg filter "${MILVUS_FILTER}" \
  '{collectionName:$collectionName, filter:$filter, outputFields:["chunk_id","content","file_id","security_level","group_id","group_name"], limit:100}')"
MILVUS_PAYLOAD_V1="$(jq -n \
  --arg collection_name "${MILVUS_COLLECTION}" \
  --arg expr "${MILVUS_FILTER}" \
  '{collection_name:$collection_name, expr:$expr, output_fields:["chunk_id","content","file_id","security_level","group_id","group_name"], limit:100}')"

MILVUS_API_VERSION=""
if curl -fsS -H 'Content-Type: application/json' -d "${MILVUS_PAYLOAD_V2}" \
  "${MILVUS_REST_URL}/v2/vectordb/entities/query" -o "${MILVUS_RESPONSE}" 2>/dev/null; then
  MILVUS_API_VERSION="v2"
elif curl -fsS -H 'Content-Type: application/json' -d "${MILVUS_PAYLOAD_V1}" \
  "${MILVUS_REST_URL}/api/v1/query" -o "${MILVUS_RESPONSE}"; then
  MILVUS_API_VERSION="v1"
fi

if [[ -n "${MILVUS_API_VERSION}" ]]; then
  echo "Milvus API selected: ${MILVUS_API_VERSION}" | tee -a "${REPORT_FILE}"

  if [[ "${MILVUS_API_VERSION}" == "v2" ]]; then
    MILVUS_CODE="$(json_get '.code' "${MILVUS_RESPONSE}")"
    MILVUS_COUNT="$(jq '.data | length' "${MILVUS_RESPONSE}" 2>/dev/null || echo 0)"
    BAD_MILVUS_ROWS="$(jq --arg fileId "${FILE_ID}" --arg securityLevel "${RAG_SECURITY_LEVEL}" --arg groupId "${GROUP_ID}" '[.data[]? | select(.file_id != $fileId or .security_level != $securityLevel or .group_id != $groupId)] | length' "${MILVUS_RESPONSE}" 2>/dev/null || echo 1)"
    if [[ -n "${EXPECTED_SNIPPET}" ]]; then
      SNIPPET_MATCHES="$(jq --arg snippet "${EXPECTED_SNIPPET}" '[.data[]? | select((.content // "" | gsub("[\\r\\n\\t ]+"; " ") | gsub("^ | $"; "")) | contains($snippet))] | length' "${MILVUS_RESPONSE}" 2>/dev/null || echo 0)"
    else
      SNIPPET_MATCHES="0"
    fi
    MILVUS_STATUS_OK="$([[ "${MILVUS_CODE}" == "0" ]] && echo "true" || echo "false")"
  else
    MILVUS_ERROR_CODE="$(jq -r '.status.error_code // 0' "${MILVUS_RESPONSE}" 2>/dev/null || echo 1)"
    MILVUS_COUNT="$(jq '(.fields_data[]? | select(.field_name=="chunk_id") | .Field.Scalars.Data.StringData.data | length) // 0' "${MILVUS_RESPONSE}" 2>/dev/null || echo 0)"
    BAD_MILVUS_ROWS="$(jq --arg fileId "${FILE_ID}" --arg securityLevel "${RAG_SECURITY_LEVEL}" --arg groupId "${GROUP_ID}" '
      def field($name): (.fields_data[] | select(.field_name == $name) | .Field.Scalars.Data.StringData.data);
      [field("file_id"), field("security_level"), field("group_id")] as $fields
      | [range(0; ($fields[0] | length)) | select($fields[0][.] != $fileId or $fields[1][.] != $securityLevel or $fields[2][.] != $groupId)]
      | length
    ' "${MILVUS_RESPONSE}" 2>/dev/null || echo 1)"
    if [[ -n "${EXPECTED_SNIPPET}" ]]; then
      SNIPPET_MATCHES="$(jq --arg snippet "${EXPECTED_SNIPPET}" '
        (.fields_data[]? | select(.field_name=="content") | .Field.Scalars.Data.StringData.data)
        | map(gsub("[\r\n\t ]+"; " ") | gsub("^ | $"; "") | select(contains($snippet))) | length
      ' "${MILVUS_RESPONSE}" 2>/dev/null || echo 0)"
    else
      SNIPPET_MATCHES="0"
    fi
    MILVUS_STATUS_OK="$([[ "${MILVUS_ERROR_CODE}" == "0" ]] && echo "true" || echo "false")"
  fi

  echo "Milvus row preview:" | tee -a "${REPORT_FILE}"
  if [[ "${MILVUS_API_VERSION}" == "v2" ]]; then
    jq -r '.data[:5][]? | "- chunk_id=\(.chunk_id) file_id=\(.file_id) security_level=\(.security_level) group_id=\(.group_id) group_name=\(.group_name) content_preview=\((.content // "") | gsub("\n"; " ") | .[0:120])"' "${MILVUS_RESPONSE}" \
      | tee -a "${REPORT_FILE}"
  else
    jq -r '
      def field($name): ((.fields_data[]? | select(.field_name == $name) | .Field.Scalars.Data.StringData.data) // []);
      [field("chunk_id"), field("content"), field("file_id"), field("security_level"), field("group_id"), field("group_name")] as $f
      | [range(0; ([($f[0] | length), 5] | min))]
      | .[]
      | "- chunk_id=\($f[0][.]) file_id=\($f[2][.]) security_level=\($f[3][.]) group_id=\($f[4][.]) group_name=\($f[5][.]) content_preview=\(($f[1][.] // "") | gsub("\n"; " ") | .[0:120])"
    ' "${MILVUS_RESPONSE}" | tee -a "${REPORT_FILE}"
  fi

  if [[ "${MILVUS_STATUS_OK}" == "true" && "${MILVUS_COUNT}" -gt 0 ]]; then
    pass "Milvus ${MILVUS_API_VERSION} API contains ${MILVUS_COUNT} chunk row(s) for file_id=${FILE_ID}"
  else
    fail "Milvus query succeeded but did not return chunk rows"
  fi

  if [[ "${BAD_MILVUS_ROWS}" == "0" ]]; then
    pass "Milvus chunk metadata matches file_id, security_level and group_id"
  else
    fail "Milvus chunk metadata has ${BAD_MILVUS_ROWS} mismatched row(s)"
  fi

  if [[ -n "${EXPECTED_SNIPPET}" ]]; then
    if [[ "${SNIPPET_MATCHES}" -gt 0 ]]; then
      pass "Milvus chunk content contains expected test-file snippet: ${EXPECTED_SNIPPET}"
    else
      fail "Milvus chunk content does not contain expected test-file snippet: ${EXPECTED_SNIPPET}"
    fi
  else
    warn "Test file snippet is empty; skipped Milvus content snippet check"
  fi
else
  fail "Milvus REST query failed at both ${MILVUS_REST_URL}/v2/vectordb/entities/query and ${MILVUS_REST_URL}/api/v1/query"
fi

section "MinIO Verification"
OBJECT_TO_CHECK="${DB_MINIO_OBJECT:-${MINIO_OBJECT_NAME}}"
if [[ -z "${OBJECT_TO_CHECK}" ]]; then
  fail "No MinIO object name found in upload response or MariaDB"
elif command -v mc >/dev/null 2>&1; then
  if mc alias set "${MINIO_ALIAS}" "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" >/dev/null; then
    if mc stat "${MINIO_ALIAS}/${MINIO_BUCKET}/${OBJECT_TO_CHECK}" | tee -a "${REPORT_FILE}" >/dev/null; then
      pass "MinIO object exists: ${MINIO_BUCKET}/${OBJECT_TO_CHECK}"
      if mc cp "${MINIO_ALIAS}/${MINIO_BUCKET}/${OBJECT_TO_CHECK}" "${MINIO_OBJECT}" >/dev/null; then
        if cmp -s "${TEST_FILE}" "${MINIO_OBJECT}"; then
          pass "MinIO backup content matches original file bytes"
        else
          fail "MinIO backup content differs from original file"
        fi
      else
        fail "Could not download MinIO object for content comparison"
      fi
    else
      fail "MinIO object not found: ${MINIO_BUCKET}/${OBJECT_TO_CHECK}"
    fi
  else
    fail "Could not configure MinIO alias ${MINIO_ALIAS}"
  fi
else
  echo "mc not installed; trying MinIO Java SDK fallback..." | tee -a "${REPORT_FILE}"
  if minio_java_verify "${OBJECT_TO_CHECK}" "${TEST_FILE}" | tee -a "${REPORT_FILE}"; then
    pass "MinIO backup object exists and content matches original file bytes"
  else
    fail "MinIO Java SDK fallback verification failed"
  fi
fi

section "Expected Values"
cat <<VALUES | tee -a "${REPORT_FILE}"
file_id=${FILE_ID}
file_name=${FILE_NAME}
security_level=${RAG_SECURITY_LEVEL}
group_id=${GROUP_ID}
group_name=${GROUP_NAME}
chunk_count=${CHUNK_COUNT}
minio_object_name=${OBJECT_TO_CHECK}
VALUES

section "Summary"
echo "PASS=${PASS_COUNT}" | tee -a "${REPORT_FILE}"
echo "FAIL=${FAIL_COUNT}" | tee -a "${REPORT_FILE}"
echo "WARN=${WARN_COUNT}" | tee -a "${REPORT_FILE}"

if (( FAIL_COUNT > 0 )); then
  exit 1
fi
