import urllib.request
import json
import urllib.error

url = "https://api.groq.com/openai/v1/chat/completions"
api_key = "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"
model = "llama-3.1-8b-instant"

headers = {
    "Authorization": f"Bearer {api_key}",
    "Content-Type": "application/json",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
}

data = {
    "model": model,
    "messages": [{"role": "user", "content": "你好，请回复'连接成功'。"}],
    "max_tokens": 50
}

req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), headers=headers, method="POST")

print(f"请求模型: {model}")
print(f"请求地址: {url}")
print("正在发送请求...\n")

try:
    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read().decode("utf-8"))
        print("✅ 验证成功！模型服务调用正常。")
        print("模型回复内容:")
        print("-" * 30)
        print(result['choices'][0]['message']['content'])
        print("-" * 30)
except urllib.error.HTTPError as e:
    print(f"❌ HTTP 错误: {e.code} {e.reason}")
    print(e.read().decode("utf-8"))
except Exception as e:
    print(f"❌ 请求失败: {e}")
