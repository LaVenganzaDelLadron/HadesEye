import requests
import time
import json
import os

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
            return json.dumps({"status": "error", "error": "Could not find job_id in response."})

        print(f"Scan started. Job ID: {job_id}")
        print(f"Report Page: https://www.hybrid-analysis.com/sample/{job_id}")

        # Poll for report with improved timeout (30 attempts * 5 seconds = 150 seconds)
        for i in range(30):
            time.sleep(5)
            check_resp = requests.get(f"{HA_BASE_URL}/report/{job_id}/summary", headers=headers)
            
            if check_resp.status_code == 200:
                summary = check_resp.json()
                verdict = summary.get("verdict")
                
                if verdict is not None:
                    threat_score = summary.get("threat_score", "N/A")
                    threat_level = summary.get("threat_level", "Unknown")

                    # Improved classification
                    if "malicious" in str(verdict).lower():
                        final_verdict = "🔥 Detected as a malicious threat."
                    elif "suspicious" in str(verdict).lower() or "pup" in str(verdict).lower():
                        final_verdict = "⚠️ Detected as suspicious/potentially unwanted."
                    else:
                        final_verdict = "✅ No signs of malware detected."

                    result = {
                        "status": "completed",
                        "threat_level": threat_level,
                        "verdict": final_verdict,
                        "threat_score": threat_score,
                        "job_id": job_id
                    }
                    return json.dumps(result)
            elif check_resp.status_code == 404:
                # Report not ready yet, continue polling
                continue
            else:
                # Unexpected status, but continue trying
                continue
        
        # If we timeout, return incomplete status
        result = {
            "status": "timeout",
            "error": "Scan did not complete within timeout period",
            "job_id": job_id
        }

    except requests.exceptions.HTTPError as err:
        result = {"status": "error", "error": f"HTTP Error: {err.response.text}"}
    except Exception as e:
        result = {"status": "error", "error": f"Exception: {str(e)}"}

    return json.dumps(result)



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
                "threat_score": "N/A",
                "ip_address": ip_address
            }
            return json.dumps(result)

        data = response.json()
        print("Response:", json.dumps(data, indent=2))

        threat_score = "10"
        threat_level = "Low"
        verdict = "✅ No known malicious activity detected."

        # Analyze response for threats
        if isinstance(data, list) and len(data) > 0:
            suspicious_count = sum(1 for item in data if "malicious" in str(item).lower() or "threat" in str(item).lower())
            
            if suspicious_count >= 5:
                threat_level = "Critical"
                threat_score = "95"
                verdict = f"🔥 IP flagged in {suspicious_count} threat reports"
            elif suspicious_count >= 2:
                threat_level = "High"
                threat_score = "80"
                verdict = f"⚠️ IP detected in {suspicious_count} threat reports"
            elif suspicious_count >= 1:
                threat_level = "Medium"
                threat_score = "60"
                verdict = "⚠️ IP has suspicious activity"
            else:
                threat_level = "Low"
                threat_score = "10"
                verdict = "✅ IP appears clean"

        result = {
            "status": "completed",
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
            "threat_score": "N/A",
            "ip_address": ip_address
        }

    except Exception as e:
        result = {
            "verdict": "⚠️ Unexpected error occurred.",
            "error": str(e),
            "threat_level": "Unknown",
            "threat_score": "N/A",
            "ip_address": ip_address
        }

    return json.dumps(result)



def scan_file_ha(api_key, file_path):
    result = {}
    try:
        if not os.path.exists(file_path):
            return json.dumps({"error": f"File not found: {file_path}"})

        headers = {
            "User-Agent": "Falcon Sandbox",
            "api-key": api_key
        }

        print(f"📤 Uploading file for full sandbox analysis: {file_path}")
        files = {"file": open(file_path, "rb")}
        data = {
            "environment_id": 300,  # Windows 10 64-bit
            "no_share_third_party": True
        }

        # Submit file
        response = requests.post(f"{HA_BASE_URL}/submit/file", headers=headers, files=files, data=data)
        response.raise_for_status()
        upload_result = response.json()
        print("Upload response:", json.dumps(upload_result, indent=2))

        job_id = upload_result.get("job_id") or upload_result.get("sha256")
        if not job_id:
            return json.dumps({"error": "Could not find job_id or sha256 in response."})

        print(f"🔍 Scan started. Job ID: {job_id}")
        print(f"🌐 Report Page: https://www.hybrid-analysis.com/sample/{job_id}")

        # Poll for report completion (longer timeout for file scans: 60 attempts * 5 seconds = 300 seconds)
        for i in range(60):
            time.sleep(5)
            check_resp = requests.get(f"{HA_BASE_URL}/report/{job_id}", headers=headers)
            
            if check_resp.status_code == 200:
                report = check_resp.json()
                verdict = report.get("verdict")
                
                if verdict is not None:
                    threat_score = report.get("threat_score", "N/A")
                    threat_level = report.get("threat_level", "Unknown")

                    if "malicious" in str(verdict).lower():
                        final_verdict = "🔥 Detected as malicious"
                    elif "suspicious" in str(verdict).lower() or "pup" in str(verdict).lower():
                        final_verdict = "⚠️ Detected as suspicious"
                    else:
                        final_verdict = "✅ No signs of malware detected"

                    result = {
                        "status": "completed",
                        "file_name": os.path.basename(file_path),
                        "verdict": final_verdict,
                        "threat_level": threat_level,
                        "threat_score": threat_score,
                        "job_id": job_id
                    }
                    return json.dumps(result)
            elif check_resp.status_code == 404:
                # Report not ready yet
                continue
            else:
                # Unexpected status, continue
                continue

        # Timeout
        result = {
            "status": "timeout",
            "file_name": os.path.basename(file_path),
            "error": "File scan did not complete within timeout",
            "job_id": job_id
        }

    except requests.exceptions.HTTPError as err:
        result = {"status": "error", "error": "HTTP Error", "details": err.response.text}
    except Exception as e:
        result = {"status": "error", "error": "Exception occurred", "details": str(e)}

    return json.dumps(result)


def scan_domain_ha(api_key, domain):
    result = {}
    try:
        headers = {
            "User-Agent": "Falcon Sandbox",
            "api-key": api_key,
            "accept": "application/json"
        }

        print(f"Scanning domain: {domain}")

        response = requests.get(
            f"{HA_BASE_URL}/search/terms",
            headers=headers,
            params={"query": domain}
        )

        if response.status_code != 200:
            result = {
                "domain": domain,
                "verdict": f"⚠️ Unable to fetch data for {domain}",
                "threat_level": "Unknown",
                "threat_score": "N/A"
            }
            return json.dumps(result)

        data = response.json()
        print("Response:", json.dumps(data, indent=2))

        threat_score = "10"
        threat_level = "Low"
        verdict = "✅ No malicious activity detected."

        # Analyze data for malicious indicators
        if isinstance(data, list) and len(data) > 0:
            malicious_count = sum(1 for item in data if "malicious" in str(item).lower())
            suspicious_count = sum(1 for item in data if "suspicious" in str(item).lower())

            if malicious_count >= 5:
                threat_level = "Critical"
                verdict = f"🔥 Domain flagged as malicious ({malicious_count} reports)"
                threat_score = "95"
            elif malicious_count >= 2 or suspicious_count >= 2:
                threat_level = "High"
                verdict = f"⚠️ Domain possibly a threat ({malicious_count} malicious, {suspicious_count} suspicious)"
                threat_score = "80"
            elif malicious_count >= 1 or suspicious_count >= 1:
                threat_level = "Medium"
                verdict = "⚠️ Domain has suspicious activity"
                threat_score = "60"
            else:
                threat_level = "Low"
                verdict = "✅ Clean domain — no malicious reports"
                threat_score = "10"

        result = {
            "status": "completed",
            "domain": domain,
            "threat_level": threat_level,
            "verdict": verdict,
            "threat_score": threat_score
        }

    except requests.exceptions.RequestException as e:
        result = {
            "domain": domain,
            "verdict": "⚠️ Network or HTTP error.",
            "error": str(e),
            "threat_level": "Unknown",
            "threat_score": "N/A"
        }

    except Exception as e:
        result = {
            "domain": domain,
            "verdict": "⚠️ Unexpected error occurred.",
            "error": str(e),
            "threat_level": "Unknown",
            "threat_score": "N/A"
        }

    return json.dumps(result, indent=2)
