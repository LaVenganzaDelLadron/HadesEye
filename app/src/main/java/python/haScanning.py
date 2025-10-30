import requests
import time


HA_BASE_URL = "https://hybrid-analysis.com/api/v2"

def scan_url_ha(api_key, url):
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
            print("Could not find job_id or sha256 in response.")
            return

        print(f"Scan started. Job ID: {job_id}")
        print(f"Report Page: https://www.hybrid-analysis.com/sample/{job_id}")

        minutes = 16 * 60
        time.sleep(minutes)

        print("Fetching final report summary...")
        report_resp = requests.get(f"{HA_BASE_URL}/report/{job_id}/summary", headers=headers)
        report_resp.raise_for_status()
        summary = report_resp.json()

        if report_resp.status_code != 200:
            verdict = summary.get("verdict", "Unknown")
            threat_score = summary.get("threat_score", "N/A")
            analysis_start_time = summary.get("analysis_start_time", "N/A")
            threat_level = summary.get("threat_level", "Unknown")


            print("\n=== Scan Summary ===")
            print(f"Verdict: {verdict}")
            print(f"Threat Level: {threat_level}")
            print(f"Threat Score: {threat_score}")
            print(f"Analysis Start Time: {analysis_start_time}")

            # Threat classification
            level_lower = str(verdict).lower()
            if any(keyword in level_lower for keyword in
                   ["malicious", "malware", "phish", "virus", "trojan", "ransom"]):
                print(f"Detected as a {verdict} threat.")
            else:
                print("No signs of phishing or malware detected.")
        else:
            print("Scan Failed")
    except Exception as e:
        print("Exception:", e)