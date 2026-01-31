#!/usr/bin/env python3
"""
Convert SVG favicon to PNG icons for all Android mipmap folders
Uses PIL to generate solid color icons from SVG (simplified approach)
"""

import os
from PIL import Image, ImageDraw

# Icon sizes for each mipmap folder (width x height in pixels)
ICON_SIZES = {
    'mipmap-mdpi': 48,      # 1x density
    'mipmap-hdpi': 72,      # 1.5x density
    'mipmap-xhdpi': 96,     # 2x density
    'mipmap-xxhdpi': 144,   # 3x density
    'mipmap-xxxhdpi': 192,  # 4x density
}

RES_DIR = r'C:\Users\OLA\AndroidStudioProjects\admob-test\app\src\main\res'

def create_icon(size, filepath):
    """Create a modern gradient icon with + and - symbols"""
    # Create image with gradient background (indigo to purple)
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Colors from SVG: Indigo (99,102,241) to Purple (139,92,246)
    # Create gradient effect by filling with base color
    for i in range(size):
        # Linear gradient from indigo to purple
        r = int(99 + (139 - 99) * (i / size))
        g = int(102 + (92 - 102) * (i / size))
        b = int(241 + (246 - 241) * (i / size))
        draw.line([(0, i), (size, i)], fill=(r, g, b, 255))
    
    # Draw circle background for design
    margin = int(size * 0.1)
    draw.ellipse(
        [(margin, margin), (size - margin, size - margin)],
        fill=(255, 255, 255, 20),
        outline=(255, 255, 255, 50),
        width=2
    )
    
    # Draw + symbol (top)
    plus_y = int(size * 0.35)
    line_width = max(2, int(size * 0.08))
    draw.rectangle(
        [(size//2 - line_width, plus_y - int(size * 0.1)), 
         (size//2 + line_width, plus_y + int(size * 0.1))],
        fill=(255, 255, 255, 220)
    )
    draw.rectangle(
        [(size//2 - int(size * 0.1), plus_y - line_width), 
         (size//2 + int(size * 0.1), plus_y + line_width)],
        fill=(255, 255, 255, 220)
    )
    
    # Draw - symbol (bottom)
    minus_y = int(size * 0.65)
    draw.rectangle(
        [(size//2 - int(size * 0.1), minus_y - line_width), 
         (size//2 + int(size * 0.1), minus_y + line_width)],
        fill=(255, 255, 255, 220)
    )
    
    # Save icon
    img.save(filepath, 'PNG')
    print(f"  ✓ Created: {os.path.basename(filepath)} ({size}x{size}px)")

def main():
    print("🎨 Android Icon Generator - PNG Creation")
    print("=" * 50)
    
    print("\nGenerating icons...")
    print("-" * 50)
    
    # Create PNG for each mipmap folder
    success_count = 0
    for folder, size in ICON_SIZES.items():
        folder_path = os.path.join(RES_DIR, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Create regular icon
        icon_path = os.path.join(folder_path, 'ic_launcher.png')
        try:
            create_icon(size, icon_path)
            success_count += 1
        except Exception as e:
            print(f"  ✗ Error creating {icon_path}: {e}")
        
        # Create round icon (same as regular for now)
        round_icon_path = os.path.join(folder_path, 'ic_launcher_round.png')
        try:
            create_icon(size, round_icon_path)
            success_count += 1
        except Exception as e:
            print(f"  ✗ Error creating {round_icon_path}: {e}")
    
    print("\n" + "=" * 50)
    print(f"✓ Icon generation complete! ({success_count}/{len(ICON_SIZES) * 2} icons created)")
    print("\nNext steps:")
    print("1. Run: .\\gradlew clean build")
    print("2. Rebuild APK")
    print("3. Icon will update in launcher!")
    
    return True

if __name__ == '__main__':
    main()
