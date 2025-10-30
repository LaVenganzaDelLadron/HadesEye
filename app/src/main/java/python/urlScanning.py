import os
import time
import requests

US_BASE_URL = "https://urlscan.io/api/v1"

def scan_url(api_key, url):
    result = {}
    try:
        headers = {
            "Accept": "application/json",
            "API-Key": api_key,
        }
        payload = {
            "url": url,
            "visibility": "public",
        }

        response = requests.post(f"{US_BASE_URL}/scan", json=payload, headers=headers)
        data = response.json()

        if data.get("message") == "Submission successful":
            uuid = data["uuid"]

            # Wait for result
            for i in range(15):
                res = requests.get(f"{US_BASE_URL}/result/{uuid}", headers=headers)
                if res.status_code == 200:
                    result_data = res.json()
                    page_info = result_data.get("page", {})
                    result = {
                        "status": "completed",
                        "uuid": uuid,
                        "title": page_info.get("title", "N/A"),
                        "domain": page_info.get("domain", "N/A"),
                        "report_url": f"https://urlscan.io/result/{uuid}/",
                    }

                    # Save screenshot
                    screenshot_url = f"https://urlscan.io/screenshots/{uuid}.png"
                    img_response = requests.get(screenshot_url)
                    if img_response.status_code == 200:
                        file_path = f"/storage/emulated/0/Download/{uuid}.png"
                        with open(file_path, "wb") as f:
                            f.write(img_response.content)
                        result["screenshot_path"] = file_path
                    else:
                        result["screenshot_path"] = "Not available"
                    break
                else:
                    time.sleep(5)
            else:
                result = {"status": "timeout", "message": "Scan not completed after waiting"}

        else:
            result = {"status": "failed", "message": data.get("message", "Unknown error")}

    except Exception as e:
        result = {"status": "error", "message": str(e)}

    return result
