const fs = require('node:fs');
const path = require('node:path');
const read = name => fs.readFileSync(path.join(__dirname, name), 'utf8');
let html = read('index.html')
  .replace('<link rel="stylesheet" href="styles.css">', `<style>${read('styles.css')}</style>`);
for (const name of ['splash-reference.png', 'wonnie-leaning.png']) {
  const uri = 'data:image/png;base64,' + fs.readFileSync(path.join(__dirname, 'assets', name)).toString('base64');
  html = html.replaceAll(`assets/${name}`, uri);
}
fs.writeFileSync(path.join(__dirname, 'preview.html'), html, 'utf8');
console.log('preview.html updated');
