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

