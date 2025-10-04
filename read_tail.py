with open(r'C:\Users\Admin\Desktop\yhchat\app\src\main\java\com\yhchat\canary\data\model\User.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()
    
total_lines = len(lines)
print(f"Total lines: {total_lines}")

# Print last 50 lines with line numbers
start = max(0, total_lines - 50)
for i in range(start, total_lines):
    print(f"{i + 1}: {lines[i].rstrip()}")