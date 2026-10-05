// Isolated frontend contract test. All API responses are test doubles, not live writes.
const {chromium}=require('playwright');
const fs=require('node:fs'); const path=require('node:path'); const assert=require('node:assert/strict');
const resources=path.resolve(__dirname,'../Application/studybuddy/studybuddy/src/main/resources');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH||'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'});
 try {
  const page=await browser.newPage({viewport:{width:1280,height:900}}); const errors=[];page.on('pageerror',e=>errors.push(e.message));
  let owner=true, failure=0, writes=0, delay=0;
  let groups=[{id:1,name:'Revision group',course:'159.302',description:'Study together',meetingTime:'Saturday',location:'Library',creatorId:1,creatorName:'Alex',createdAt:'2026-09-27T10:00:00Z',memberCount:2,joined:true}];
  await page.route('http://groups.test/**',async route=>{
   const p=new URL(route.request().url()).pathname;const method=route.request().method();
   const json=(data,status=200)=>route.fulfill({status,contentType:'application/json',body:JSON.stringify(data)});
   if(p==='/api/auth/me')return json({id:owner?1:2});
   if(p==='/api/groups')return json(groups);
   if(p==='/api/groups/1/members')return json([{userId:1,firstName:'Alex',lastName:'Chen',creator:true}]);
   if(p==='/api/groups/1/posts')return json([{id:1,authorName:'Sam',content:'Hello',createdAt:'2026-09-27T10:00:00Z'}]);
   if(p==='/api/groups/1'){
    writes++;assert.equal(route.request().headers()['x-csrf-token'],'test-token');
    if(delay)await new Promise(r=>setTimeout(r,delay));
    if(failure)return json({message:'Request rejected'},failure);
    if(method==='PUT'){const body=route.request().postDataJSON();assert.deepEqual(Object.keys(body).sort(),['course','description','location','meetingTime','name']);groups=[{...groups[0],...body}];return json(groups[0]);}
    if(method==='DELETE'){assert.equal(route.request().postData(),null);groups=[];return route.fulfill({status:204,body:''});}
   }
   if(p.startsWith('/js/')||p.startsWith('/css/'))return route.fulfill({contentType:p.endsWith('.js')?'text/javascript':'text/css',body:fs.readFileSync(path.join(resources,'static',p),'utf8')});
   let html=fs.readFileSync(path.join(resources,'templates/groups.html'),'utf8');
   html=html.replace('<head>','<head><meta name="_csrf" content="test-token"><meta name="_csrf_header" content="X-CSRF-TOKEN">');
   return route.fulfill({contentType:'text/html',body:html});
  });
  const has=async(selector,text)=>page.locator(selector).filter({hasText:text}).waitFor();
  await page.goto('http://groups.test/groups');await page.getByRole('button',{name:'Edit group',exact:true}).click();
  assert.equal(await page.inputValue('#edit-group-name'),'Revision group');
  await page.locator('#edit-group-dialog').getByRole('button',{name:'Cancel'}).click();assert.equal(writes,0);
  await page.getByRole('button',{name:'Edit group',exact:true}).click();await page.fill('#edit-group-name','  Updated group  ');failure=405;
  await page.getByRole('button',{name:'Save changes'}).click();await has('#edit-group-status','not available yet');assert.equal(await page.inputValue('#edit-group-name'),'  Updated group  ');assert.equal(groups[0].name,'Revision group');
  failure=400;await page.getByRole('button',{name:'Save changes'}).click();await has('#edit-group-status','Request rejected');
  failure=0;delay=300;await page.getByRole('button',{name:'Save changes'}).click();assert.equal(await page.getByRole('button',{name:'Save changes'}).isDisabled(),true);
  await page.keyboard.press('Escape');assert.equal(await page.locator('#edit-group-dialog').evaluate(n=>n.open),true);await has('#group-action-status','updated successfully');assert.equal(groups[0].name,'Updated group');delay=0;
  await page.getByRole('button',{name:'Open group'}).click();await has('#group-detail-title','Updated group');
  await page.getByRole('button',{name:'Delete group',exact:true}).click();const before=writes;await page.getByRole('button',{name:'Delete permanently'}).click();assert.equal(writes,before);
  await page.locator('#delete-group-dialog').getByRole('button',{name:'Cancel'}).click();assert.equal(writes,before);
  await page.getByRole('button',{name:'Delete group',exact:true}).click();await page.check('#confirm-group-delete');failure=403;
  await page.getByRole('button',{name:'Delete permanently'}).click();await has('#delete-group-status','not permitted');assert.equal(groups.length,1);assert.equal(await page.locator('#group-detail').isVisible(),true);
  await page.setViewportSize({width:390,height:844});await page.waitForTimeout(600);await page.screenshot({path:'/tmp/groups-delete-dialog.png'});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
  failure=0;await page.getByRole('button',{name:'Delete permanently'}).click();await has('#group-action-status','deleted successfully');await has('#groups-status','No groups');assert.equal(await page.locator('#group-detail').isVisible(),false);
  groups=[{id:1,name:'Member group',course:'159.302',description:'Study',creatorId:1,creatorName:'Alex',memberCount:2,joined:true}];owner=false;await page.reload();await has('#groups-status','1 group');assert.equal(await page.getByRole('button',{name:'Edit group',exact:true}).count(),0);assert.equal(await page.getByRole('button',{name:'Delete group',exact:true}).count(),0);
  owner=true;await page.reload();await page.getByRole('button',{name:'Edit group',exact:true}).click();await page.screenshot({path:'/tmp/groups-edit-dialog.png'});assert.deepEqual(errors,[]);
  console.log('PASS: creator-only controls, prefill/cancel, PUT payload and CSRF, 405/400 input retention, pending protection, delete confirmation, 403 retention, 204 removal, mobile dialogs.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
