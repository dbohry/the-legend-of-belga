#!/bin/bash

# Native Build Script for The Legend of Belga
# This script builds native executables for different platforms

set -e

echo "🏗️  Building Native Executable for The Legend of Belga"
echo "=================================================="

# Check if GraalVM is available
if ! command -v native-image &> /dev/null; then
    echo "❌ Error: GraalVM Native Image not found!"
    echo "Please install GraalVM and run: gu install native-image"
    echo "See NATIVE_BUILD_README.md for detailed instructions"
    exit 1
fi

# Detect platform
PLATFORM=$(uname -s)
echo "📱 Detected platform: $PLATFORM"

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build native executable
echo "🔨 Building native executable..."
./gradlew nativeCompile

# Copy to appropriate location based on platform
case "$PLATFORM" in
    "Darwin")
        echo "🍎 macOS detected - copying executable..."
        cp build/native/nativeCompile/TheLegendOfBelga build/TheLegendOfBelga-macOS
        chmod +x build/TheLegendOfBelga-macOS
        echo "✅ Native executable created: build/TheLegendOfBelga-macOS"
        ;;
    "Linux")
        echo "🐧 Linux detected - copying executable..."
        cp build/native/nativeCompile/TheLegendOfBelga build/TheLegendOfBelga-Linux
        chmod +x build/TheLegendOfBelga-Linux
        echo "✅ Native executable created: build/TheLegendOfBelga-Linux"
        ;;
    "MINGW"*|"MSYS"*|"CYGWIN"*)
        echo "🪟 Windows detected - copying executable..."
        cp build/native/nativeCompile/TheLegendOfBelga.exe build/TheLegendOfBelga-Windows.exe
        echo "✅ Native executable created: build/TheLegendOfBelga-Windows.exe"
        ;;
    *)
        echo "⚠️  Unknown platform: $PLATFORM"
        echo "Native executable should be in: build/native/nativeCompile/"
        ;;
esac

echo ""
echo "🎉 Build completed successfully!"
echo ""
echo "📁 Your native executable is ready in the build/ directory"
echo "🚀 Users can now run the game without Java installed!"
echo ""
echo "📖 For more information, see: NATIVE_BUILD_README.md"
