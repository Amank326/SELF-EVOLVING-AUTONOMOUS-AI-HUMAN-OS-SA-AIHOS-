#!/bin/bash
# SA-AIHOS Start Script

echo "🚀 Starting SA-AIHOS Application Stack..."

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js first."
    exit 1
fi

# Check if npm packages are installed
if [ ! -d "backend/node_modules" ]; then
    echo "📦 Installing backend dependencies..."
    cd backend
    npm install
    cd ..
fi

# Start backend server
echo "Starting backend server on port 3000..."
cd backend
npm start &
BACKEND_PID=$!
cd ..

echo "✅ Backend server started (PID: $BACKEND_PID)"

# Wait a moment for backend to start
sleep 3

# Start frontend server
echo "Starting frontend server on port 8080..."
cd ..
python server.py &
FRONTEND_PID=$!

echo "✅ Frontend server started (PID: $FRONTEND_PID)"

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║              SA-AIHOS Application Stack Started            ║"
echo "║                                                            ║"
echo "║  Frontend:  http://localhost:8080/home.html              ║"
echo "║  Backend:   http://localhost:3000                        ║"
echo "║  WebSocket: ws://localhost:3000                          ║"
echo "║                                                            ║"
echo "║  Press Ctrl+C to stop all services                        ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Cleanup function
cleanup() {
    echo ""
    echo "Shutting down services..."
    kill $BACKEND_PID 2>/dev/null
    kill $FRONTEND_PID 2>/dev/null
    echo "✅ All services stopped"
}

# Trap Ctrl+C
trap cleanup INT TERM

# Wait for both processes
wait
