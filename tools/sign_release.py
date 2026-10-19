"""Privately sign a verified CI release candidate; never upload the private key to GitHub."""
import argparse, hashlib, os, re, subprocess
from pathlib import Path

p=argparse.ArgumentParser()
p.add_argument('candidate',type=Path)
p.add_argument('--keystore',type=Path,required=True)
p.add_argument('--password-file',type=Path,required=True)
p.add_argument('--output',type=Path,required=True)
p.add_argument('--sdk',type=Path,default=Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or ''))
a=p.parse_args()
jars=sorted((a.sdk/'build-tools').glob('*/lib/apksigner.jar'))
if not jars:raise SystemExit('Install Android SDK build tools or provide --sdk')
a.output.parent.mkdir(parents=True,exist_ok=True)
java=['java','-Djava.io.tmpdir='+str(a.output.parent.resolve()),'-jar',str(jars[-1])]
subprocess.run(java+['sign','--ks',str(a.keystore),'--ks-key-alias','tvbridge-release','--ks-pass','file:'+str(a.password_file),'--key-pass','file:'+str(a.password_file),'--out',str(a.output),str(a.candidate)],check=True)
verified=subprocess.check_output(java+['verify','--verbose','--print-certs',str(a.output)],text=True)
expected=(Path(__file__).resolve().parents[1]/'release/signing-certificate.sha256').read_text().strip().lower()
actual=re.search(r'certificate SHA-256 digest:\s*([a-fA-F0-9]+)',verified)
if not actual or actual.group(1).lower()!=expected:raise SystemExit('Wrong production certificate. Do not publish.')
(a.output.parent/'SIGNING.txt').write_text(verified,encoding='utf-8')
digest=hashlib.sha256(a.output.read_bytes()).hexdigest()
(a.output.parent/'SHA256SUMS.txt').write_text(digest+'  '+a.output.name+'\n',encoding='ascii')
print('Production signature verified; APK, SIGNING.txt and SHA256SUMS.txt are ready.')
