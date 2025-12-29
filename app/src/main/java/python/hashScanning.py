import requests
import json

VT_BASE_URL = "https://www.virustotal.com/api/v3"

def scan_hash(api_key, file_hash):
    """Scan file hash (MD5, SHA1, SHA256) against VirusTotal"""
    headers = {"x-apikey": api_key}
    result = {}

    try:
        # Normalize hash
        file_hash = file_hash.strip().lower()
        
        response = requests.get(f"{VT_BASE_URL}/files/{file_hash}", headers=headers)
        response.raise_for_status()

        data = response.json().get("data", {}).get("attributes", {})
        last_analysis_stats = data.get("last_analysis_stats", {})
        
        malicious = last_analysis_stats.get("malicious", 0)
        suspicious = last_analysis_stats.get("suspicious", 0)
        harmless = last_analysis_stats.get("harmless", 0)
        undetected = last_analysis_stats.get("undetected", 0)
        
        # Get file info
        file_names = data.get("names", [])
        file_size = data.get("size", 0)
        file_type = data.get("type_description", "Unknown")
        
        if malicious >= 5:
            verdict = "🔥 Malicious hash detected!"
        elif malicious >= 2 or suspicious >= 2:
            verdict = "⚠️ Suspicious hash"
        else:
            verdict = "✅ Clean hash"

        result = {
            "status": "completed",
            "hash": file_hash,
            "malicious": str(malicious),
            "suspicious": str(suspicious),
            "harmless": str(harmless),
            "undetected": str(undetected),
            "verdict": verdict,
            "file_names": ", ".join(file_names[:3]) if file_names else "Unknown",
            "file_size": str(file_size),
            "file_type": file_type
        }

    except requests.exceptions.HTTPError as e:
        if e.response.status_code == 404:
            result = {
                "status": "not_found",
                "hash": file_hash,
                "verdict": "✅ Hash not found in database (likely clean)"
            }
        else:
            result = {"status": "error", "error": f"Request failed: {str(e)}"}
    except Exception as e:
        result = {"status": "error", "error": f"Unexpected error: {str(e)}"}

    return json.dumps(result)
