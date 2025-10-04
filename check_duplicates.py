# Python script to check for duplicate class definitions in User.kt
import re

file_path = r'C:\Users\Admin\Desktop\yhchat\app\src\main\java\com\yhchat\canary\data\model\User.kt'

# Read the file
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Look for duplicate class definitions
duplicate_classes = ['EditHistoryItem', 'ListEditRequest', 'ListEditMessage', 'ListEditData']

print("Checking for duplicate class definitions...")

for class_name in duplicate_classes:
    # Find all occurrences of the class definition
    pattern = rf'data class {class_name}\s*\('
    matches = list(re.finditer(pattern, content))
    
    if len(matches) > 1:
        print(f"Found {len(matches)} occurrences of {class_name}:")
        for i, match in enumerate(matches):
            line_number = content[:match.start()].count('\n') + 1
            print(f"  {i+1}. Line {line_number}")
    elif len(matches) == 1:
        print(f"Found 1 occurrence of {class_name} (no duplicates)")
    else:
        print(f"No occurrences of {class_name} found")