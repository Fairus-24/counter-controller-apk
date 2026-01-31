#!/usr/bin/env python3
"""
Copy favicon.png to all mipmap folders with proper resizing
"""

import os
from PIL import Image

FAVICON_PNG = r'C:\Users\OLA\AndroidStudioProjects\admob-test\app\src\main\assets\favicon.png'
RES_DIR = r'C:\Users\OLA\AndroidStudioProjects\admob-test\app\src\main\res'

# Icon sizes for each mipmap folder
ICON_SIZES = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

def process_icon(favicon_path, output_path, size):
    """Resize and save PNG icon"""
    try:
        # Open favicon
        img = Image.open(favicon_path)
        
        # Resize to target size (using high-quality resampling)
        img_resized = img.resize((size, size), Image.Resampling.LANCZOS)
        
        # Save PNG
        img_resized.save(output_path, 'PNG')
        print(f"  ✓ Created: {os.path.basename(output_path)} ({size}x{size}px)")
        return True
    except Exception as e:
        print(f"  ✗ Error: {e}")
        return False

def main():
    print("🎨 Copy Favicon to Android Launcher Icons")
    print("=" * 50)
    
    # Check if favicon exists
    if not os.path.exists(FAVICON_PNG):
        print(f"✗ Favicon not found: {FAVICON_PNG}")
        return False
    
    print(f"Source: {FAVICON_PNG}\n")
    
    print("Copying and resizing icons...")
    print("-" * 50)
    
    success = 0
    for folder, size in ICON_SIZES.items():
        folder_path = os.path.join(RES_DIR, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Process both regular and round icons
        icon_path = os.path.join(folder_path, 'ic_launcher.png')
        if process_icon(FAVICON_PNG, icon_path, size):
            success += 1
        
        round_icon_path = os.path.join(folder_path, 'ic_launcher_round.png')
        if process_icon(FAVICON_PNG, round_icon_path, size):
            success += 1
    
    print("\n" + "=" * 50)
    print(f"✓ Complete! ({success}/{len(ICON_SIZES) * 2} icons copied)")
    print("\nNext steps:")
    print("1. Run: .\\gradlew clean build")
    print("2. adb uninstall app.counter.controller.caba")
    print("3. .\\gradlew installDebug")
    
    return True

if __name__ == '__main__':
    main()
