"""Cross-check the complete QR symbol against an independent implementation."""
import subprocess, sys, segno
urls = ['http://192.168.1.100:8765', 'http://10.0.0.2:8765', 'http://172.31.255.254:8765']
lines = subprocess.check_output(['java', '-cp', sys.argv[1], 'io.github.smileplzz.tvbridge.QrDump', *urls], text=True).splitlines()
for i, url in enumerate(urls):
    assert lines[i * 34] == url
    expected = segno.make(url, version=4, error='L', mode='byte', mask=0, boost_error=False)
    actual = lines[i * 34 + 1:i * 34 + 34]
    assert actual == [''.join(str(int(v)) for v in row) for row in expected.matrix], f'QR mismatch: {url}'
print('3 QR matrices match independent Segno implementation')
