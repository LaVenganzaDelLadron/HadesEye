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
                "threat_level": "N/A",
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

    return json.dumps(result)



def scan_file_ha(api_key, file_path):
    result = {}
    try:
        if not os.path.exists(file_path):
            return {"error": f"File not found: {file_path}"}

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
            return {"error": "Could not find job_id or sha256 in response."}

        print(f"🔍 Scan started. Job ID: {job_id}")
        print(f"🌐 Report Page: https://www.hybrid-analysis.com/sample/{job_id}")

        # Poll for report completion
        while True:
            time.sleep(20)
            check_resp = requests.get(f"{HA_BASE_URL}/report/{job_id}", headers=headers)
            if check_resp.status_code == 200:
                report = check_resp.json()

                # Print once to inspect full structure
                # (You can remove this later)
                print(json.dumps(report, indent=2))

                verdict = report.get("verdict")
                threat_score = report.get("threat_score", "N/A")
                threat_level = report.get("threat_level", "Unknown")

                if verdict is not None:
                    if "malicious" in str(verdict).lower():
                        result = {
                            "file_name": os.path.basename(file_path),
                            "verdict": "⚠️ Detected as malicious",
                            "threat_level": threat_level,
                            "threat_score": threat_score
                        }
                    else:
                        result = {
                            "file_name": os.path.basename(file_path),
                            "verdict": "✅ No signs of malware detected",
                            "threat_level": threat_level,
                            "threat_score": threat_score
                        }
                    break
                else:
                    print("⏳ Scan still processing...")
            elif check_resp.status_code == 404:
                print("⏳ Report not ready yet...")
            else:
                print(f"⚠️ Unexpected status: {check_resp.status_code}")
                result = {"error": f"Unexpected status: {check_resp.status_code}"}
                break

    except requests.exceptions.HTTPError as err:
        result = {"error": "HTTP Error", "details": err.response.text}
    except Exception as e:
        result = {"error": "Exception occurred", "details": str(e)}

    return result


def scan_domain_ha(api_key, domain):
    result = {}
    try:
        headers = {
            "User-Agent": "Falcon Sandbox",
            "api-key": api_key,
            "accept": "application/json"
        }

        print(f"Scanning domain: {domain}")

        # Hybrid Analysis does not have a direct /domain endpoint
        # but we can use the /search/terms endpoint to look for reputation data
        response = requests.get(
            f"{HA_BASE_URL}/search/terms",
            headers=headers,
            params={"query": domain}
        )

        if response.status_code != 200:
            result = {
                "domain": domain,
                "verdict": f"⚠️ Unable to fetch data for {domain}",
                "threat_level": "N/A",
                "threat_score": "N/A"
            }
            return json.dumps(result)

        data = response.json()
        print("Response:", json.dumps(data, indent=2))

        threat_score = "N/A"
        threat_level = "Unknown"
        verdict = "✅ No malicious activity detected."

        # Analyze data for malicious indicators
        if isinstance(data, list) and len(data) > 0:
            malicious_count = sum(1 for item in data if "malicious" in str(item).lower())
            suspicious_count = sum(1 for item in data if "suspicious" in str(item).lower())

            if malicious_count >= 5:
                threat_level = "Critical"
                verdict = f"🔥 Domain flagged as malicious ({malicious_count} reports)"
                threat_score = "95"
            elif malicious_count >= 2 or suspicious_count >= 1:
                threat_level = "High"
                verdict = f"⚠️ Domain possibly a threat ({malicious_count} malicious, {suspicious_count} suspicious)"
                threat_score = "80"
            elif suspicious_count == 1:
                threat_level = "Medium"
                verdict = "⚠️ Domain has suspicious activity"
                threat_score = "60"
            else:
                threat_level = "Low"
                verdict = "✅ Clean domain — no malicious reports"
                threat_score = "10"

        result = {
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
