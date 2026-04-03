#!/usr/bin/env python3
"""
SA-AIHOS Web Server
Simple HTTP server for running the web application locally
"""

import http.server
import socketserver
import os
import sys
from pathlib import Path

class MyHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        """Add headers to prevent caching during development"""
        self.send_header('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        super().end_headers()

    def do_GET(self):
        """Handle GET requests"""
        if self.path == '/':
            self.path = '/index.html'
        return super().do_GET()

    def log_message(self, format, *args):
        """Custom logging"""
        print(f"[{self.log_date_time_string()}] {format % args}")

def run_server(port=8080, host='localhost'):
    """Run the web server"""
    web_dir = Path(__file__).parent
    os.chdir(web_dir)
    
    handler = MyHTTPRequestHandler
    
    try:
        with socketserver.TCPServer((host, port), handler) as httpd:
            server_address = f"http://{host}:{port}"
            print(f"""
╔══════════════════════════════════════════════════════════════╗
║           SA-AIHOS Web Application Server                    ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  Server running on: {server_address:<40} ║
║  Working directory: {str(web_dir):<30} ║
║                                                              ║
║  Open your browser and navigate to:                         ║
║  → {server_address:<45} ║
║                                                              ║
║  Press Ctrl+C to stop the server                            ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
            """)
            httpd.serve_forever()
            
    except KeyboardInterrupt:
        print("\n\n✓ Server stopped gracefully")
    except OSError as e:
        print(f"✗ Error: {e}")
        if "Address already in use" in str(e):
            print(f"  Port {port} is already in use. Try a different port:")
            print(f"  python server.py --port 8081")
        sys.exit(1)

if __name__ == '__main__':
    port = 8080
    host = 'localhost'
    
    # Parse arguments
    for arg in sys.argv[1:]:
        if arg.startswith('--port='):
            port = int(arg.split('=')[1])
        elif arg.startswith('--host='):
            host = arg.split('=')[1]
    
    run_server(port, host)
