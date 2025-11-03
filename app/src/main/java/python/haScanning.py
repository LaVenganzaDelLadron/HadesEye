import requests
import time
import json

HA_BASE_URL = "https://hybrid-analysis.com/api/v2"

def scan_url_ha(api_key, url):
    result = {}
    try:
        headers = {
            "User-Agent": "Falcon Sandbox",
            "api-key": api_key,
            "Content-Type": "application/x-www-form-urlencoded"
        }

        data = {
            "url": url,
            "scan_type": "all"
        }

        print(f"Submitting URL for analysis: {url}")
        response = requests.post(f"{HA_BASE_URL}/quick-scan/url", headers=headers, data=data)
        response.raise_for_status()
        result = response.json()
        print("Response:", result)

        job_id = result.get("job_id") or result.get("sha256") or result.get("id")
        if not job_id:
            result = {"Could not find job_id or sha256 in response."}
            return

        print(f"Scan started. Job ID: {job_id}")
        print(f"Report Page: https://www.hybrid-analysis.com/sample/{job_id}")

        while True:
            time.sleep(10)
            check_resp = requests.get(f"{HA_BASE_URL}/report/{job_id}/summary", headers=headers)
            if check_resp.status_code == 200:
                summary = check_resp.json()

                verdict = summary.get("verdict")
                if verdict is not None:
                    # Scan done
                    threat_score = summary.get("threat_score", "N/A")
                    threat_level = summary.get("threat_level", "Unknown")

                    result = {
                        "threat_level": threat_level,
                        "verdict": verdict,
                        "threat_score": threat_score,
                    }


                    if "malicious" in str(verdict).lower():
                        result = {
                            "threat_level": threat_level,
                            "verdict": "⚠️ Detected as a malicious threat.",
                            "threat_score": threat_score
                        }
                    else:
                        result = {
                            "threat_level": threat_level,
                            "verdict": "✅ No signs of phishing or malware detected.",
                            "threat_score": threat_score
                        }

                    break
                else:
                    result = {"⏳ Scan still processing, waiting..."}
            elif check_resp.status_code == 404:
                result = {"⏳ Report not ready yet..."}
            else:
                result = {f"⚠️ Unexpected status: {check_resp.status_code}"}
                break

    except requests.exceptions.HTTPError as err:
        result = {"HTTP Error:", err.response.text}
    except Exception as e:
        result = {"Exception:", e}

    return result



def scan_ip_ha(api_key, ip_address):
    result = {}
    try:
        headers = {
            "User-Agent": "Falcon Sandbox",
            "api-key": api_key,
            "accept": "application/json"
        }

        print(f"Scanning IP address: {ip_address}")

        response = requests.get(f"{HA_BASE_URL}/search/hash", headers=headers, params={"query": ip_address})

        if response.status_code != 200:
            result = {
                "verdict": f"⚠️ Unable to fetch data for {ip_address}",
                "threat_level": "Unknown",
                "threat_score": "N/A"
            }
            return json.dumps(result)  # ✅ Return JSON

        data = response.json()
        print("Response:", json.dumps(data, indent=2))

        threat_score = "N/A"
        threat_level = "Unknown"
        verdict = "✅ No known malicious activity detected."

        if isinstance(data, list) and len(data) > 0:
            suspicious_count = sum(1 for item in data if "malicious" in str(item).lower())
            if suspicious_count > 0:
                verdict = f"⚠️ Detected in {suspicious_count} analysis reports."
                threat_level = "High"
                threat_score = "90"
            else:
                threat_level = "Low"
                threat_score = "10"

        result = {
            "ip_address": ip_address,
            "threat_level": threat_level,
            "verdict": verdict,
            "threat_score": threat_score
        }

    except requests.exceptions.RequestException as e:
        result = {
            "verdict": "⚠️ Network or HTTP error.",
            "error": str(e),
            "threat_level": "Unknown",
            "threat_score": "N/A"
        }

    except Exception as e:
        result = {
            "verdict": "⚠️ Unexpected error occurred.",
            "error": str(e),
            "threat_level": "Unknown",
            "threat_score": "N/A"
        }

    return json.dumps(result)  # ✅ Always return as JSON string