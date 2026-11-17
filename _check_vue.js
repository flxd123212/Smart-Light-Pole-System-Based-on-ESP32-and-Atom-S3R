
const fs = require('fs');
const path = require('path');
const compiler = require('./start-vue/node_modules/vue-template-compiler');
const parser = require('./start-vue/node_modules/@babel/parser');

const files = [
  'start-vue/src/views/IotCameraCapture/components/LiveMonitor.vue',
  'start-vue/src/views/IotCameraCapture/IotCameraCapture/index.vue',
  'start-vue/src/views/index.vue'
];
let bad = 0;
for (const f of files) {
  const src = fs.readFileSync(f, 'utf8');
  const sfc = compiler.parseComponent(src);
  const tpl = compiler.compile(sfc.template.content);
  if (tpl.errors && tpl.errors.length) { bad++; console.log('TEMPLATE ERROR', f, tpl.errors); }
  try {
    parser.parse(sfc.script.content, { sourceType: 'module', plugins: ['objectRestSpread','optionalChaining','nullishCoalescingOperator','dynamicImport'] });
  } catch (e) { bad++; console.log('SCRIPT ERROR', f, e.message); }
  console.log((tpl.errors.length?'FAIL':'OK  '), f, '| styles:', sfc.styles.length);
}
console.log(bad === 0 ? 'ALL GOOD' : 'ERRORS: ' + bad);
