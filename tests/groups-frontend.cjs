// Run against an isolated local app with H2. Creates two disposable test accounts.
// Requires Node, Playwright and Chrome. See Documents/groups-frontend-handoff.md.
const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const base = process.env.STUDYBUDDY_TEST_URL || 'http://localhost:18081';
if (!['localhost', '127.0.0.1'].includes(new URL(base).hostname)) throw new Error('Use a local test server only.');
(async () => {
 const browser = await chromium.launch({headless:true, executablePath:process.env.CHROME_PATH || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'});
 try {
  const errors=[];
  async function account(firstName) {
   const context=await browser.newContext(); const page=await context.newPage();
   page.on('pageerror',error=>errors.push(error.message));
   await page.goto(base+'/register');
   const email=`${firstName.toLowerCase()}-${Date.now()}@example.com`;
   const result=await page.evaluate(async ({firstName,email})=>{
    const token=document.querySelector('meta[name="_csrf"]').content;
    const header=document.querySelector('meta[name="_csrf_header"]').content;
    const response=await fetch('/api/auth/register',{method:'POST',headers:{'Content-Type':'application/json',[header]:token},body:JSON.stringify({firstName,lastName:'Frontend Test',email,password:'Password123!',confirmPassword:'Password123!'})});
    return response.status;
   },{firstName,email});
   assert.equal(result,201);
   await page.goto(base+'/login');await page.fill('#email',email);await page.fill('#password','Password123!');
   await page.locator('[data-auth-form] button[type="submit"]').click();await page.waitForURL('**/profile');
   await page.goto(base+'/groups');return page;
  }
  const status=async(page,id,text)=>page.locator(id).filter({hasText:text}).waitFor();
  const a=await account('Alex'); const b=await account('Sam');
  const name=`Revision ${Date.now()} <b>group</b>`;
  await a.fill('#group-name',name);await a.fill('#group-course','159.302');await a.fill('#group-description','Practice search together');
  await a.fill('#meeting-time','Saturday afternoon');await a.fill('#location','Library');
  await a.locator('#create-group-form button[type="submit"]').click();await status(a,'#group-form-status','Group created');
  const ownerCard=a.locator('.group-card').filter({hasText:name});await ownerCard.waitFor();
  assert.equal(await ownerCard.getByRole('button',{name:'Leave group',exact:true}).count(),0);
  assert.equal(await ownerCard.locator('h3 b').count(),0);
  await b.click('#refresh-groups');const card=b.locator('.group-card').filter({hasText:name});await card.getByRole('button',{name:'Open group'}).click();
  await status(b,'#post-access-note','Join this group');assert.equal(await b.locator('#group-post-form').isVisible(),false);
  await status(b,'#group-members','Alex Frontend Test');await status(b,'#posts-status','No posts');
  await card.getByRole('button',{name:'Join group',exact:true}).click();await status(b,'#group-action-status','You joined');
  await b.locator('#group-post-form').waitFor();await b.fill('#group-post-content','Shall we meet at 2 pm? <script>unsafe</script>');
  await b.locator('#group-post-form button').click();await status(b,'#post-form-status','Message posted');
  await ownerCard.getByRole('button',{name:'Open group'}).click();await status(a,'#group-posts','Shall we meet');
  assert.equal(await a.locator('#group-posts script').count(),0);
  await b.reload();const refreshed=b.locator('.group-card').filter({hasText:name});await refreshed.getByRole('button',{name:'Open group'}).click();await status(b,'#group-posts','Shall we meet');
  await b.route('**/api/groups/*/posts',route=>route.request().method()==='POST'?route.fulfill({status:500,contentType:'application/json',body:'{}'}):route.continue());
  await b.fill('#group-post-content','Keep this failed draft');await b.locator('#group-post-form button').click();await status(b,'#post-form-status','temporarily unavailable');assert.equal(await b.inputValue('#group-post-content'),'Keep this failed draft');
  await b.unroute('**/api/groups/*/posts');
  await refreshed.getByRole('button',{name:'Leave group',exact:true}).click();await status(b,'#group-action-status','You left');assert.equal(await b.locator('#group-post-form').isVisible(),false);
  // Check server enforcement too: hiding the form is not authorization.
  const rejected=await b.evaluate(async()=>{
   const token=document.querySelector('meta[name="_csrf"]').content;const header=document.querySelector('meta[name="_csrf_header"]').content;
   const response=await fetch(`/api/groups/${selectedGroupId}/posts`,{method:'POST',headers:{'Content-Type':'application/json',[header]:token},body:JSON.stringify({content:'Not allowed'})});return response.status;
  });assert.equal(rejected,403);
  // Exercise empty, service failure, retry, and expired session without altering real data.
  await b.route('**/api/groups',route=>route.fulfill({status:200,contentType:'application/json',body:'[]'}));await b.click('#refresh-groups');await status(b,'#groups-status','No groups available');await b.unroute('**/api/groups');
  await b.route('**/api/groups',route=>route.fulfill({status:503,contentType:'application/json',body:'{}'}));await b.click('#refresh-groups');await status(b,'#groups-status','temporarily unavailable');await b.unroute('**/api/groups');
  await b.click('#refresh-groups');await b.locator('.group-card').filter({hasText:name}).waitFor();
  await b.route('**/api/groups',route=>route.fulfill({status:401,contentType:'application/json',body:'{}'}));await b.click('#refresh-groups');await b.locator('#groups-status a[href="/login"]').waitFor();await b.unroute('**/api/groups');
  await a.setViewportSize({width:1280,height:900});await a.screenshot({path:'/tmp/studybuddy-groups-final-desktop.png',fullPage:true});
  await a.setViewportSize({width:390,height:844});await a.screenshot({path:'/tmp/studybuddy-groups-final-mobile.png',fullPage:true});assert.equal(await a.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
  for(const url of ['/','/matches','/groups','/sessions','/profile']) {const response=await a.goto(base+url);assert.equal(response.status(),200,`Navigation ${url}`);}
  assert.deepEqual(errors,[]);
  console.log('PASS: real two-user create/join/members/posts/leave/persistence/permissions; simulated error/empty/401 states; navigation and mobile layout.');
 } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exit(1)});
