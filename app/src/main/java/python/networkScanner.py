import socket
import json
from concurrent.futures import ThreadPoolExecutor, as_completed

def scan_port(ip, port, timeout=1):
    """Scan a single port"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(timeout)
        result = sock.connect_ex((ip, port))
        sock.close()
        return port if result == 0 else None
    except:
        return None


def scan_network(target_ip, ports_to_scan=None):
    """Scan network for open ports"""
    result = {}
    
    try:
        # Common ports if not specified
        if ports_to_scan is None:
            ports_to_scan = [21, 22, 23, 25, 53, 80, 110, 135, 139, 143, 443, 445, 
                           993, 995, 1433, 1521, 3306, 3389, 5432, 5900, 8080, 8443]
        
        open_ports = []
        
        # Scan ports using thread pool
        with ThreadPoolExecutor(max_workers=50) as executor:
            future_to_port = {
                executor.submit(scan_port, target_ip, port): port 
                for port in ports_to_scan
            }
            
            for future in as_completed(future_to_port):
                port_result = future.result()
                if port_result:
                    open_ports.append(port_result)
        
        open_ports.sort()
        
        # Get service names for common ports
        port_services = {
            21: "FTP", 22: "SSH", 23: "Telnet", 25: "SMTP", 53: "DNS",
            80: "HTTP", 110: "POP3", 135: "RPC", 139: "NetBIOS", 143: "IMAP",
            443: "HTTPS", 445: "SMB", 993: "IMAPS", 995: "POP3S",
            1433: "MSSQL", 1521: "Oracle", 3306: "MySQL", 3389: "RDP",
            5432: "PostgreSQL", 5900: "VNC", 8080: "HTTP-Alt", 8443: "HTTPS-Alt"
        }
        
        services_found = [f"{port} ({port_services.get(port, 'Unknown')})" for port in open_ports]
        
        # Vulnerability assessment
        dangerous_ports = [21, 23, 135, 139, 445, 3389]
        dangerous_open = [p for p in open_ports if p in dangerous_ports]
        
        if len(dangerous_open) > 0:
            verdict = f"⚠️ {len(dangerous_open)} vulnerable ports detected!"
            risk_level = "High"
        elif len(open_ports) > 10:
            verdict = f"⚠️ {len(open_ports)} ports open - review security"
            risk_level = "Medium"
        elif len(open_ports) > 0:
            verdict = f"✅ {len(open_ports)} ports open - normal"
            risk_level = "Low"
        else:
            verdict = "✅ No open ports detected"
            risk_level = "Safe"
        
        result = {
            "status": "completed",
            "ip": target_ip,
            "open_ports_count": str(len(open_ports)),
            "open_ports": ", ".join(map(str, open_ports)),
            "services": ", ".join(services_found) if services_found else "None",
            "vulnerable_ports": ", ".join(map(str, dangerous_open)) if dangerous_open else "None",
            "verdict": verdict,
            "risk_level": risk_level
        }
        
    except Exception as e:
        result = {"status": "error", "error": f"Scan error: {str(e)}"}
    
    return json.dumps(result)


def get_local_ip():
    """Get local IP address"""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return json.dumps({"status": "success", "local_ip": ip})
    except:
        return json.dumps({"status": "error", "error": "Unable to get local IP"})
