import requests
import json
import time
import os

VT_BASE_URL = "https://www.virustotal.com/api/v3"

def scan_url(api_key, scan_url):
    headers = {"x-apikey": api_key}
    result = {}

    try:
        response = requests.post(f"{VT_BASE_URL}/urls", headers=headers, data={"url": scan_url})
        response.raise_for_status()
        analysis_id = response.json()["data"]["id"]

        # Poll with longer timeout (max 120 seconds) - wait first before checking
        for i in range(24):  # 24 iterations * 5 seconds = 120 seconds
            time.sleep(5)
            report = requests.get(f"{VT_BASE_URL}/analyses/{analysis_id}", headers=headers)
            report.raise_for_status()
            data = report.json()["data"]["attributes"]

            if data["status"] == "completed":
                stats = data.get("stats", {})
                malicious = stats.get("malicious", 0)
                suspicious = stats.get("suspicious", 0)
                harmless = stats.get("harmless", 0)
                undetected = stats.get("undetected", 0)
                
                # Improved verdict logic
                if malicious >= 5:
                    verdict = "🔥 Malicious - Multiple vendors detected threats"
                elif malicious >= 2:
                    verdict = "⚠️ Suspicious - Some vendors flagged as malicious"
                elif suspicious >= 2:
                    verdict = "⚠️ Suspicious - Multiple vendors flagged as suspicious"
                else:
                    verdict = "✅ Safe - No significant threats detected"
                
                result = {
                    "status": "completed",
                    "malicious": str(malicious),
                    "suspicious": str(suspicious),
                    "harmless": str(harmless),
                    "undetected": str(undetected),
                    "verdict": verdict
                }
                return json.dumps(result)

        # If still not completed after timeout
        result = {"status": "timeout", "error": "Analysis did not complete within timeout period"}

    except requests.exceptions.RequestException as e:
        result = {"status": "error", "error": f"Request failed: {str(e)}"}
    except KeyError as e:
        result = {"status": "error", "error": f"Unexpected response format: {str(e)}"}
    except Exception as e:
        result = {"status": "error", "error": f"Unexpected error: {str(e)}"}

    return json.dumps(result)

def scan_ip(api_key, ip):
    headers = {"x-apikey": api_key}
    result = {}

    try:
        response = requests.get(f"{VT_BASE_URL}/ip_addresses/{ip}", headers=headers)
        response.raise_for_status()

        data = response.json().get("data", {}).get("attributes", {})
        last_analysis_stats = data.get("last_analysis_stats", {})
        
        malicious = last_analysis_stats.get("malicious", 0)
        suspicious = last_analysis_stats.get("suspicious", 0)
        harmless = last_analysis_stats.get("harmless", 0)
        undetected = last_analysis_stats.get("undetected", 0)
        reputation = data.get("reputation", 0)

        # Improved verdict logic
        if malicious >= 5:
            verdict = "🔥 Malicious IP - Multiple vendors flagged"
        elif malicious >= 2:
            verdict = "⚠️ Suspicious IP - Some vendors flagged"
        elif suspicious >= 2:
            verdict = "⚠️ Suspicious IP - Suspicious activity detected"
        else:
            verdict = "✅ Safe IP - No known malicious activity"

        result = {
            "status": "completed",
            "malicious": str(malicious),
            "suspicious": str(suspicious),
            "harmless": str(harmless),
            "undetected": str(undetected),
            "reputation": str(reputation),
            "verdict": verdict
        }

    except requests.exceptions.RequestException as e:
        result = {"status": "error", "error": f"Request failed: {str(e)}"}
    except KeyError as e:
        result = {"status": "error", "error": f"Unexpected response format: {str(e)}"}
    except Exception as e:
        result = {"status": "error", "error": f"Unexpected error: {str(e)}"}

    return json.dumps(result)


def scan_file(api_key, file_path):
    headers = {"x-apikey": api_key}
    result = {}

    try:
        with open(file_path, "rb") as f:
            files = {"file": (os.path.basename(file_path), f)}
            response = requests.post(f"{VT_BASE_URL}/files", headers=headers, files=files)
            response.raise_for_status()
            analysis_id = response.json()["data"]["id"]

        # Poll with longer timeout (max 180 seconds for files)
        for i in range(36):  # 36 iterations * 5 seconds = 180 seconds
            time.sleep(5)
            report = requests.get(f"{VT_BASE_URL}/analyses/{analysis_id}", headers=headers)
            report.raise_for_status()
            data = report.json()["data"]["attributes"]

            if data["status"] == "completed":
                stats = data.get("stats", {})
                malicious = stats.get("malicious", 0)
                suspicious = stats.get("suspicious", 0)
                harmless = stats.get("harmless", 0)
                undetected = stats.get("undetected", 0)
                
                # Improved verdict logic
                if malicious >= 5:
                    verdict = "🔥 Malicious - Multiple vendors detected threats"
                elif malicious >= 2:
                    verdict = "⚠️ Suspicious - Some vendors flagged as malicious"
                elif suspicious >= 2:
                    verdict = "⚠️ Suspicious - Multiple vendors flagged as suspicious"
                else:
                    verdict = "✅ Safe - No significant threats detected"
                
                result = {
                    "status": "completed",
                    "malicious": str(malicious),
                    "suspicious": str(suspicious),
                    "harmless": str(harmless),
                    "undetected": str(undetected),
                    "verdict": verdict
                }
                return json.dumps(result)

        # If still not completed after timeout
        result = {"status": "timeout", "error": "File analysis did not complete within timeout period"}

    except requests.exceptions.RequestException as e:
        result = {"status": "error", "error": f"Request failed: {str(e)}"}
    except Exception as e:
        result = {"status": "error", "error": f"An error occurred: {str(e)}"}

    return json.dumps(result)

def scan_domain(api_key, domain):
    headers = {"x-apikey": api_key}
    result = {}

    try:
        print(f"Scanning domain: {domain}")
        response = requests.get(f"{VT_BASE_URL}/domains/{domain}", headers=headers)
        response.raise_for_status()

        data = response.json().get("data", {}).get("attributes", {})
        last_analysis_stats = data.get("last_analysis_stats", {})

        # Extract info
        malicious = last_analysis_stats.get("malicious", 0)
        suspicious = last_analysis_stats.get("suspicious", 0)
        harmless = last_analysis_stats.get("harmless", 0)
        undetected = last_analysis_stats.get("undetected", 0)
        reputation = data.get("reputation", 0)
        
        result = {
            "status": "completed",
            "domain": domain,
            "malicious": str(malicious),
            "suspicious": str(suspicious),
            "harmless": str(harmless),
            "undetected": str(undetected),
            "reputation": str(reputation)
        }

        # Classification with improved accuracy
        if malicious >= 5:
            result["verdict"] = "🔥 Malicious domain detected!"
        elif malicious >= 2 or suspicious >= 2:
            result["verdict"] = "⚠️ Possibly dangerous domain."
        else:
            result["verdict"] = "✅ Clean domain — no threats found."

    except requests.exceptions.RequestException as e:
        result = {"status": "error", "error": f"Request failed: {str(e)}"}
    except KeyError as e:
        result = {"status": "error", "error": f"Unexpected response format: {str(e)}"}
    except Exception as e:
        result = {"status": "error", "error": f"Unexpected error: {str(e)}"}

    return json.dumps(result, indent=2)