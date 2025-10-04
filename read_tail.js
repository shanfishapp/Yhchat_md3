const fs = require('fs');

// Read the file
const filePath = 'C:/Users/Admin/Desktop/yhchat/app/src/main/java/com/yhchat/canary/data/model/User.kt';
const data = fs.readFileSync(filePath, 'utf8');

// Split into lines
const lines = data.split('
');
const totalLines = lines.length;

console.log(`Total lines: ${totalLines}`);

// Print last 50 lines with line numbers
const start = Math.max(0, totalLines - 50);
for (let i = start; i < totalLines; i++) {
    console.log(`${i + 1}: ${lines[i]}`);
}