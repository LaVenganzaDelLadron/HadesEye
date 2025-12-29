import requests
import hashlib
import json

def check_password_breach(password):
    """Check if password is in known breaches using HaveIBeenPwned API"""
    result = {}
    try:
        # Hash password using SHA-1
        sha1_hash = hashlib.sha1(password.encode()).hexdigest().upper()
        prefix = sha1_hash[:5]
        suffix = sha1_hash[5:]
        
        # Query HaveIBeenPwned API
        response = requests.get(f"https://api.pwnedpasswords.com/range/{prefix}")
        response.raise_for_status()
        
        # Check if hash suffix exists in response
        hashes = response.text.split('\r\n')
        breach_count = 0
        
        for hash_line in hashes:
            if ':' in hash_line:
                hash_suffix, count = hash_line.split(':')
                if hash_suffix == suffix:
                    breach_count = int(count)
                    break
        
        if breach_count > 0:
            if breach_count >= 100000:
                verdict = f"🔥 CRITICAL: Password found in {breach_count:,} breaches!"
                strength = "Very Weak"
            elif breach_count >= 10000:
                verdict = f"⚠️ WARNING: Password found in {breach_count:,} breaches"
                strength = "Weak"
            else:
                verdict = f"⚠️ Password found in {breach_count:,} breaches"
                strength = "Compromised"
        else:
            verdict = "✅ Password not found in known breaches"
            strength = "Good"
        
        result = {
            "status": "completed",
            "breach_count": str(breach_count),
            "verdict": verdict,
            "strength": strength
        }
        
    except Exception as e:
        result = {"status": "error", "error": f"Error: {str(e)}"}
    
    return json.dumps(result)


def check_email_breach(email):
    """Check if email is in known data breaches"""
    result = {}
    try:
        headers = {"User-Agent": "HadesEye-Security-App"}
        response = requests.get(
            f"https://haveibeenpwned.com/api/v3/breachedaccount/{email}",
            headers=headers
        )
        
        if response.status_code == 200:
            breaches = response.json()
            breach_count = len(breaches)
            breach_names = [b.get("Name", "Unknown") for b in breaches[:5]]
            
            verdict = f"⚠️ Email found in {breach_count} data breaches!"
            result = {
                "status": "breached",
                "breach_count": str(breach_count),
                "breaches": ", ".join(breach_names),
                "verdict": verdict
            }
        elif response.status_code == 404:
            result = {
                "status": "safe",
                "breach_count": "0",
                "verdict": "✅ Email not found in known breaches"
            }
        else:
            result = {"status": "error", "error": "Unable to check email"}
            
    except Exception as e:
        result = {"status": "error", "error": f"Error: {str(e)}"}
    
    return json.dumps(result)
