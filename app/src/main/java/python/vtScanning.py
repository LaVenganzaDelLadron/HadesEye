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

        for _ in range(30):
            report = requests.get(f"{VT_BASE_URL}/analyses/{analysis_id}", headers=headers)
            report.raise_for_status()
            data = report.json()["data"]["attributes"]

            if data["status"] == "completed":
                stats = data.get("stats", {})
                result = {
                    "status": data["status"],
                    "data": stats.get("data", 0),
                    "malicious": stats.get("malicious", 0),
                    "harmless": stats.get("harmless", 0),
                    "suspicious": stats.get("suspicious", 0),
                    "undetected": stats.get("undetected", 0)
                }
                break

            time.sleep(5)

    except requests.exceptions.RequestException as e:
        result = {"error": f"Request failed: {e}"}
    except KeyError:
        result = {"error": "Unexpected response from VirusTotal"}

    return json.dumps(result)

def scan_ip(api_key, ip):
    headers = {"x-apikey": api_key}
    result = {}

    try:
        response = requests.get(f"{VT_BASE_URL}/ip_addresses/{ip}", headers=headers)
        response.raise_for_status()

        data = response.json().get("data", {}).get("attributes", {})
        last_analysis_stats = data.get("last_analysis_stats", {})

        result = {
            "status": "completed",
            "malicious": last_analysis_stats.get("malicious", 0),
            "harmless": last_analysis_stats.get("harmless", 0),
            "suspicious": last_analysis_stats.get("suspicious", 0),
            "undetected": last_analysis_stats.get("undetected", 0)
        }

    except requests.exceptions.RequestException as e:
        result = {"error": f"Request failed: {e}"}
    except KeyError:
        result = {"error": "Unexpected response from VirusTotal"}

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

        for _ in range(30):
            report = requests.get(f"{VT_BASE_URL}/analyses/{analysis_id}", headers=headers)
            report.raise_for_status()
            data = report.json()["data"]["attributes"]

            if data["status"] == "completed":
                stats = data.get("stats", {})
                result = {
                    "status": data["status"],
                    "data": stats.get("data", 0),
                    "malicious": stats.get("malicious", 0),
                    "harmless": stats.get("harmless", 0),
                    "suspicious": stats.get("suspicious", 0),
                    "undetected": stats.get("undetected", 0)
                }
                break

            time.sleep(5)
        else:
            result = {"error": "Scan timed out or still in progress."}

    except requests.exceptions.RequestException as e:
        result = {"error": f"Request failed: {e}"}
    except Exception as e:
        result = {"error": f"An error occurred: {e}"}

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
        result = {
            "domain": domain,
            "malicious": last_analysis_stats.get("malicious", 0),
            "harmless": last_analysis_stats.get("harmless", 0),
            "suspicious": last_analysis_stats.get("suspicious", 0),
            "undetected": last_analysis_stats.get("undetected", 0),
            "categories": data.get("categories", {}),
            "reputation": data.get("reputation", 0),
            "whois": data.get("whois", "No WHOIS data available.")
        }

        # Classification
        malicious = last_analysis_stats.get("malicious", 0)
        suspicious = last_analysis_stats.get("suspicious", 0)

        if malicious >= 5:
            result["verdict"] = "🔥 Malicious domain detected!"
        elif malicious >= 2 or suspicious >= 1:
            result["verdict"] = "⚠️ Possibly dangerous domain."
        else:
            result["verdict"] = "✅ Clean domain — no threats found."

    except requests.exceptions.RequestException as e:
        result = {"error": f"Request failed: {e}"}
    except KeyError:
        result = {"error": "Unexpected response from VirusTotal"}

    return json.dumps(result, indent=2)