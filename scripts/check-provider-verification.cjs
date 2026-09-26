// Usage: node scripts/check-provider-verification.cjs PATH_TO_INSPECTED_PUBLIC_BUNDLE
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),crypto=require('node:crypto');
(async()=>{
 const path=process.argv[2];
 assert(path,'Provide a local copy of the inspected public bundle');
 const bytes=fs.readFileSync(path);
 const sha256=crypto.createHash('sha256').update(bytes).digest('hex');
 assert.equal(sha256,'bbc0e8291f932f91f1f3dbbea3bdb7adae4332b6af990027823089b0ca3f497a');
 const js=bytes.toString('utf8'),prefix='let Cb=(()=>{',start=js.indexOf(prefix),end=js.indexOf('return A.\\u0275fac=',start);
 assert(start>=0&&end>start);
 const source=js.slice(start+prefix.length,end),events=[];
 const Component=vm.runInNewContext('('+source+')',{qa:class{emit(x){events.push(x)}},console:{log(){}}});
 let pending,resets=0;
 const component=new Component({show(){},hide(){}},{login(user,response){assert.equal(user,'recaptcha');assert.equal(response,'synthetic-response');return {subscribe(success,error){pending={success,error}}}}},{setToken(value){assert.equal(value,'synthetic-token')}},{openSnackBar(){}},{});
 component.recaptchaRef={reset(){resets++}};
 component.resolved('synthetic-response');
 assert.notEqual(component.recaptchaSuccess,true);assert.deepEqual(events,[]);
 pending.success({token:'synthetic-token'});await new Promise(setImmediate);
 assert.equal(component.recaptchaSuccess,true);assert.deepEqual(events,[true]);
 component.resolved(null);assert.equal(component.recaptchaSuccess,false);assert.equal(events.at(-1),false);
 component.resolved('synthetic-response');pending.error({synthetic:true});await new Promise(setImmediate);
 assert.equal(component.recaptchaSuccess,false);assert.equal(events.at(-1),false);
 component.recaptchaSuccess=true;component.errored({synthetic:true});assert.equal(component.recaptchaSuccess,false);
 const report={source:'https://pasarelaspe.sistarbanc.com.uy/v2/main-es2015.c4dd4374250f3678bcc8.js',sha256,angular:'11.2.14',acceptedOnlyAfterProvider:true,expiryResets:true,rejectionResets:true,widgetErrorResets:true,resets,networkRequests:0,credentials:'synthetic only'};
 fs.mkdirSync('outputs',{recursive:true});
 fs.writeFileSync('outputs/provider-verification-logic.json',JSON.stringify(report,null,2)+'\n');console.log(report);
})().catch(e=>{console.error(e);process.exitCode=1});
