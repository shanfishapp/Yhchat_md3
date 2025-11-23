#!/bin/bash
# batch_convert.sh

INPUT_DIR="./"
OUTPUT_DIR="./"
mkdir -p "$OUTPUT_DIR"

# 批量转换所有 SVG 文件
for svg_file in "$INPUT_DIR"/*.svg; do
    if [[ -f "$svg_file" ]]; then
        # 获取文件名（不含扩展名）
        filename=$(basename "$svg_file" .svg)
        output_file="$OUTPUT_DIR/${filename}.png"
        
        echo "转换: $svg_file → $output_file"
        resvg "$svg_file" "$output_file"
    fi
done

echo "✅ 批量转换完成！"